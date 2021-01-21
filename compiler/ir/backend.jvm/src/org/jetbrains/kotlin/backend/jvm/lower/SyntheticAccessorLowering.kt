/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.backend.jvm.intrinsics.receiverAndArgs
import org.jetbrains.kotlin.backend.jvm.ir.IrInlineReferenceLocator
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.hasMangledParameters
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class SyntheticAccessorLowering(val context: JvmBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {
    data class LambdaCallSite(val scope: IrDeclaration, val crossinline: Boolean)

    private val pendingAccessorsToAdd = mutableListOf<IrFunction>()
    private val inlineLambdaToCallSite = mutableMapOf<IrFunction, LambdaCallSite>()

    override fun lower(irFile: IrFile) {
        irFile.accept(object : IrInlineReferenceLocator(context) {
            override fun visitInlineLambda(
                argument: IrFunctionReference,
                callee: IrFunction,
                parameter: IrValueParameter,
                scope: IrDeclaration
            ) {
                // suspendCoroutine and suspendCoroutineUninterceptedOrReturn accept crossinline lambdas to disallow non-local returns,
                // but these lambdas are effectively inline
                inlineLambdaToCallSite[argument.symbol.owner] =
                    LambdaCallSite(scope, parameter.isCrossinline && !callee.isCoroutineIntrinsic())
            }
        }, null)

        irFile.transformChildrenVoid(this)

        for (accessor in pendingAccessorsToAdd) {
            assert(accessor.fileOrNull == irFile || accessor.isAllowedToBeAddedToForeignFile()) {
                "SyntheticAccessorLowering should not attempt to modify other files!\n" +
                        "While lowering this file: ${irFile.render()}\n" +
                        "Trying to add this accessor: ${accessor.render()}"
            }
            (accessor.parent as IrDeclarationContainer).declarations.add(accessor)
        }
    }

    private data class FieldKey(val fieldSymbol: IrFieldSymbol, val parent: IrDeclarationParent, val superQualifierSymbol: IrClassSymbol?)

    private val functionMap = mutableMapOf<Pair<IrFunctionSymbol, IrDeclarationParent>, IrFunctionSymbol>()
    private val getterMap = mutableMapOf<FieldKey, IrSimpleFunctionSymbol>()
    private val setterMap = mutableMapOf<FieldKey, IrSimpleFunctionSymbol>()

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        if (expression.usesDefaultArguments()) {
            return super.visitFunctionAccess(expression)
        }

        val callee = expression.symbol.owner
        val withSuper = (expression as? IrCall)?.superQualifierSymbol != null
        val thisSymbol = (expression as? IrCall)?.dispatchReceiver?.type?.classifierOrNull as? IrClassSymbol

        val accessor = when {
            callee is IrConstructor && callee.isOrShouldBeHidden ->
                handleHiddenConstructor(callee).symbol

            !expression.symbol.isAccessible(withSuper, thisSymbol) -> {
                // Find the right container to insert the accessor. Simply put, when we call a function on a class A,
                // we also need to put its accessor into A. However, due to the way that calls are implemented in the
                // IR we generally need to look at the type of the dispatchReceiver *argument* in order to find the
                // correct class. Consider the following code:
                //
                //     fun run(f : () -> Int): Int = f()
                //
                //     open class A {
                //         private fun f() = 0
                //         fun g() = run { this.f() }
                //     }
                //
                //     class B : A {
                //         override fun g() = 1
                //         fun h() = run { super.g() }
                //     }
                //
                // We have calls to the private methods A.f from a generated Lambda subclass for the argument to `run`
                // in class A and a super call to A.g from a generated Lambda subclass in class B.
                //
                // In the first case, we need to produce an accessor in class A to access the private member of A.
                // Both the parent of the function f and the type of the dispatch receiver point to the correct class.
                // In the second case we need to call A.g from within class B, since this is the only way to invoke
                // a method of a superclass on the JVM. However, the IR for the call to super.g points directly to the
                // function g in class A. Confusingly, the `superQualifier` on this call also points to class A.
                // The only way to compute the actual enclosing class for the call is by looking at the type of the
                // dispatch receiver argument, which points to B.
                //
                // Beyond this, there can be accessors that are needed because other lowerings produce code calling
                // private methods (e.g., local functions for lambdas are private and called from generated
                // SAM wrapper classes). In this case we rely on the parent field of the called function.
                //
                // Finally, we need to produce accessors for calls to protected static methods coming from Java,
                // which we put in the closest enclosing class which has access to the method in question.
                val symbol = expression.symbol
                val dispatchReceiverType = expression.dispatchReceiver?.type
                val parent = symbol.owner.accessorParent(dispatchReceiverType?.classOrNull?.owner ?: symbol.owner.parent)

                // The key in the cache/map needs to be BOTH the symbol of the function being accessed AND the parent
                // of the accessor. Going from the above example, if we have another class C similar to B:
                //
                //     class C : A {
                //         override fun g() = 2
                //         fun i() = run { super.g() }
                //     }
                //
                // For the call to super.g in function i, the accessor to A.g must be produced in C. Therefore, we
                // cannot use the function symbol (A.g in the example) by itself as the key since there should be
                // one accessor per dispatch receiver (i.e., parent of the accessor).
                functionMap.getOrPut(symbol to parent) {
                    when (symbol) {
                        is IrConstructorSymbol -> symbol.owner.makeConstructorAccessor().also(pendingAccessorsToAdd::add).symbol
                        is IrSimpleFunctionSymbol -> symbol.owner.makeSimpleFunctionAccessor(expression as IrCall, parent).symbol
                        else -> error("Unknown subclass of IrFunctionSymbol")
                    }
                }
            }

            else -> return super.visitFunctionAccess(expression)
        }
        return super.visitExpression(modifyFunctionAccessExpression(expression, accessor))
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        val dispatchReceiverType = expression.receiver?.type
        return super.visitExpression(
            if (!expression.symbol.isAccessible(false, dispatchReceiverType?.classifierOrNull as? IrClassSymbol)) {
                val symbol = expression.symbol
                val parent =
                    symbol.owner.accessorParent(dispatchReceiverType?.classOrNull?.owner ?: symbol.owner.parent) as IrClass
                modifyGetterExpression(
                    expression,
                    getterMap.getOrPut(FieldKey(symbol, parent, expression.superQualifierSymbol)) {
                        makeGetterAccessorSymbol(symbol, parent, expression.superQualifierSymbol)
                    }
                )
            } else {
                expression
            }
        )
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        val dispatchReceiverType = expression.receiver?.type
        return super.visitExpression(
            if (!expression.symbol.isAccessible(false, dispatchReceiverType?.classifierOrNull as? IrClassSymbol)) {
                val symbol = expression.symbol
                val parent =
                    symbol.owner.accessorParent(dispatchReceiverType?.classOrNull?.owner ?: symbol.owner.parent) as IrClass
                modifySetterExpression(
                    expression,
                    setterMap.getOrPut(FieldKey(symbol, parent, expression.superQualifierSymbol)) {
                        makeSetterAccessorSymbol(symbol, parent, expression.superQualifierSymbol)
                    }
                )
            } else {
                expression
            }
        )
    }

    override fun visitConstructor(declaration: IrConstructor): IrStatement {
        if (declaration.isOrShouldBeHidden) {
            pendingAccessorsToAdd.add(handleHiddenConstructor(declaration))
            declaration.visibility = DescriptorVisibilities.PRIVATE
        }

        return super.visitConstructor(declaration)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val function = expression.symbol.owner

        if (!expression.origin.isLambda && function is IrConstructor && function.isOrShouldBeHidden) {
            handleHiddenConstructor(function).let { accessor ->
                expression.transformChildrenVoid()
                return IrFunctionReferenceImpl(
                    expression.startOffset, expression.endOffset, expression.type,
                    accessor.symbol, accessor.typeParameters.size,
                    accessor.valueParameters.size, accessor.symbol, expression.origin
                )
            }
        }

        return super.visitFunctionReference(expression)
    }

    private val IrConstructor.isOrShouldBeHidden: Boolean
        get() {
            if (this in context.hiddenConstructors)
                return true

            if (origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
                origin == JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR ||
                origin == JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR_FOR_HIDDEN_CONSTRUCTOR
            ) {
                return false
            }

            val constructedClass = constructedClass

            if (!DescriptorVisibilities.isPrivate(visibility) && !constructedClass.isInline && hasMangledParameters)
                return true

            if (visibility != DescriptorVisibilities.PUBLIC && constructedClass.modality == Modality.SEALED)
                return true

            return false
        }

    private fun handleHiddenConstructor(declaration: IrConstructor): IrConstructor {
        require(declaration.isOrShouldBeHidden, declaration::render)
        return context.hiddenConstructors.getOrPut(declaration) {
            declaration.makeConstructorAccessor(JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR_FOR_HIDDEN_CONSTRUCTOR).also { accessor ->
                if (declaration.constructedClass.modality != Modality.SEALED) {
                    // There's a special case in the JVM backend for serializing the metadata of hidden
                    // constructors - we serialize the descriptor of the original constructor, but the
                    // signature of the accessor. We implement this special case in the JVM IR backend by
                    // attaching the metadata directly to the accessor. We also have to move all annotations
                    // to the accessor. Parameter annotations are already moved by the copyTo method.
                    if (declaration.metadata != null) {
                        accessor.metadata = declaration.metadata
                        declaration.metadata = null
                    }
                    accessor.annotations += declaration.annotations
                    declaration.annotations = emptyList()
                    declaration.valueParameters.forEach { it.annotations = emptyList() }
                }
            }
        }
    }

    // In case of Java `protected static`, access could be done from a public inline function in the same package,
    // or a subclass of the Java class. Both cases require an accessor, which we cannot add to the Java class.
    private fun IrDeclarationWithVisibility.accessorParent(parent: IrDeclarationParent = this.parent) =
        if (visibility == JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY) {
            val classes = allScopes.map { it.irElement }.filterIsInstance<IrClass>()
            val companions = classes.mapNotNull(IrClass::companionObject)
            val objectsInScope =
                classes.flatMap { it.declarations.filter(IrDeclaration::isAnonymousObject).filterIsInstance<IrClass>() }
            val candidates = objectsInScope + companions + classes
            candidates.lastOrNull { parent is IrClass && it.isSubclassOf(parent) } ?: classes.last()
        } else parent

    private fun IrConstructor.makeConstructorAccessor(
        originForConstructorAccessor: IrDeclarationOrigin =
            JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
    ): IrConstructor {
        val source = this

        return factory.buildConstructor {
            origin = originForConstructorAccessor
            name = source.name
            visibility = DescriptorVisibilities.PUBLIC
        }.also { accessor ->
            accessor.parent = source.parent

            accessor.copyTypeParametersFrom(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.copyValueParametersToStatic(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)
            if (source.constructedClass.modality == Modality.SEALED) {
                for (accessorValueParameter in accessor.valueParameters) {
                    accessorValueParameter.annotations = emptyList()
                }
            }

            accessor.returnType = source.returnType.remapTypeParameters(source, accessor)

            accessor.addValueParameter(
                "constructor_marker".synthesizedString,
                context.ir.symbols.defaultConstructorMarker.defaultType.makeNullable(),
                JvmLoweredDeclarationOrigin.SYNTHETIC_MARKER_PARAMETER
            )

            accessor.body = IrExpressionBodyImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                createConstructorCall(accessor, source.symbol)
            )
        }
    }

    private fun createConstructorCall(accessor: IrConstructor, targetSymbol: IrConstructorSymbol) =
        IrDelegatingConstructorCallImpl.fromSymbolOwner(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            context.irBuiltIns.unitType,
            targetSymbol, targetSymbol.owner.parentAsClass.typeParameters.size + targetSymbol.owner.typeParameters.size
        ).also {
            copyAllParamsToArgs(it, accessor)
        }

    private fun IrSimpleFunction.makeSimpleFunctionAccessor(expression: IrCall, parent: IrDeclarationParent): IrSimpleFunction {
        val source = this

        return factory.buildFun {
            startOffset = parent.startOffset
            endOffset = parent.startOffset
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = source.accessorName(expression.superQualifierSymbol)
            visibility = DescriptorVisibilities.PUBLIC
            modality = if (parent is IrClass && parent.isJvmInterface) Modality.OPEN else Modality.FINAL
            isSuspend = source.isSuspend // synthetic accessors of suspend functions are handled in codegen
        }.also { accessor ->
            accessor.parent = parent
            pendingAccessorsToAdd.add(accessor)

            accessor.copyTypeParametersFrom(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.copyValueParametersToStatic(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR, expression.dispatchReceiver?.type)
            accessor.returnType = source.returnType.remapTypeParameters(source, accessor)

            accessor.body = IrExpressionBodyImpl(
                accessor.startOffset, accessor.startOffset,
                createSimpleFunctionCall(accessor, source.symbol, expression.superQualifierSymbol)
            )
        }
    }

    private fun createSimpleFunctionCall(accessor: IrFunction, targetSymbol: IrSimpleFunctionSymbol, superQualifierSymbol: IrClassSymbol?) =
        IrCallImpl.fromSymbolOwner(
            accessor.startOffset,
            accessor.endOffset,
            accessor.returnType,
            targetSymbol, targetSymbol.owner.typeParameters.size,
            superQualifierSymbol = superQualifierSymbol
        ).also {
            copyAllParamsToArgs(it, accessor)
        }

    private fun makeGetterAccessorSymbol(
        fieldSymbol: IrFieldSymbol,
        parent: IrClass,
        superQualifierSymbol: IrClassSymbol?
    ): IrSimpleFunctionSymbol =
        context.irFactory.buildFun {
            startOffset = parent.startOffset
            endOffset = parent.startOffset
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = fieldSymbol.owner.accessorNameForGetter(superQualifierSymbol)
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            returnType = fieldSymbol.owner.type
        }.also { accessor ->
            accessor.parent = parent
            pendingAccessorsToAdd.add(accessor)

            if (!fieldSymbol.owner.isStatic) {
                // Accessors are always to one's own fields.
                accessor.addValueParameter(
                    "\$this", parent.defaultType, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
                )
            }

            accessor.body = createAccessorBodyForGetter(fieldSymbol.owner, accessor, superQualifierSymbol)
        }.symbol

    private fun createAccessorBodyForGetter(
        targetField: IrField,
        accessor: IrSimpleFunction,
        superQualifierSymbol: IrClassSymbol?
    ): IrBody {
        val maybeDispatchReceiver =
            if (targetField.isStatic) null
            else IrGetValueImpl(accessor.startOffset, accessor.endOffset, accessor.valueParameters[0].symbol)
        return IrExpressionBodyImpl(
            accessor.startOffset, accessor.endOffset,
            IrGetFieldImpl(
                accessor.startOffset, accessor.endOffset,
                targetField.symbol,
                targetField.type,
                maybeDispatchReceiver,
                superQualifierSymbol = superQualifierSymbol
            )
        )
    }

    private fun makeSetterAccessorSymbol(
        fieldSymbol: IrFieldSymbol,
        parent: IrClass,
        superQualifierSymbol: IrClassSymbol?
    ): IrSimpleFunctionSymbol =
        context.irFactory.buildFun {
            startOffset = parent.startOffset
            endOffset = parent.startOffset
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = fieldSymbol.owner.accessorNameForSetter(superQualifierSymbol)
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            returnType = context.irBuiltIns.unitType
        }.also { accessor ->
            accessor.parent = parent
            pendingAccessorsToAdd.add(accessor)

            if (!fieldSymbol.owner.isStatic) {
                // Accessors are always to one's own fields.
                accessor.addValueParameter(
                    "\$this", parent.defaultType, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
                )
            }

            accessor.addValueParameter("<set-?>", fieldSymbol.owner.type, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)

            accessor.body = createAccessorBodyForSetter(fieldSymbol.owner, accessor, superQualifierSymbol)
        }.symbol

    private fun createAccessorBodyForSetter(
        targetField: IrField,
        accessor: IrSimpleFunction,
        superQualifierSymbol: IrClassSymbol?
    ): IrBody {
        val maybeDispatchReceiver =
            if (targetField.isStatic) null
            else IrGetValueImpl(accessor.startOffset, accessor.endOffset, accessor.valueParameters[0].symbol)
        val value = IrGetValueImpl(
            accessor.startOffset, accessor.endOffset,
            accessor.valueParameters[if (targetField.isStatic) 0 else 1].symbol
        )
        return IrExpressionBodyImpl(
            accessor.startOffset, accessor.endOffset,
            IrSetFieldImpl(
                accessor.startOffset, accessor.endOffset,
                targetField.symbol,
                maybeDispatchReceiver,
                value,
                context.irBuiltIns.unitType,
                superQualifierSymbol = superQualifierSymbol
            )
        )
    }

    private fun modifyFunctionAccessExpression(
        oldExpression: IrFunctionAccessExpression,
        accessorSymbol: IrFunctionSymbol
    ): IrFunctionAccessExpression {
        val newExpression = when (oldExpression) {
            is IrCall -> IrCallImpl.fromSymbolOwner(
                oldExpression.startOffset, oldExpression.endOffset,
                oldExpression.type,
                accessorSymbol as IrSimpleFunctionSymbol, oldExpression.typeArgumentsCount,
                origin = oldExpression.origin
            )
            is IrDelegatingConstructorCall -> IrDelegatingConstructorCallImpl.fromSymbolOwner(
                oldExpression.startOffset, oldExpression.endOffset,
                context.irBuiltIns.unitType,
                accessorSymbol as IrConstructorSymbol, oldExpression.typeArgumentsCount
            )
            is IrConstructorCall ->
                IrConstructorCallImpl.fromSymbolOwner(
                    oldExpression.startOffset, oldExpression.endOffset,
                    oldExpression.type,
                    accessorSymbol as IrConstructorSymbol
                )
            else ->
                error("Unexpected IrFunctionAccessExpression: $oldExpression")
        }
        newExpression.copyTypeArgumentsFrom(oldExpression)
        val receiverAndArgs = oldExpression.receiverAndArgs()
        receiverAndArgs.forEachIndexed { i, irExpression ->
            newExpression.putValueArgument(i, irExpression)
        }
        if (accessorSymbol is IrConstructorSymbol) {
            newExpression.putValueArgument(
                receiverAndArgs.size,
                IrConstImpl.constNull(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.ir.symbols.defaultConstructorMarker.defaultType.makeNullable()
                )
            )
        }
        return newExpression
    }

    private fun modifyGetterExpression(
        oldExpression: IrGetField,
        accessorSymbol: IrSimpleFunctionSymbol
    ): IrCall {
        val call = IrCallImpl(
            oldExpression.startOffset, oldExpression.endOffset,
            oldExpression.type,
            accessorSymbol, 0, accessorSymbol.owner.valueParameters.size,
            oldExpression.origin
        )
        oldExpression.receiver?.let {
            call.putValueArgument(0, oldExpression.receiver)
        }
        return call
    }

    private fun modifySetterExpression(
        oldExpression: IrSetField,
        accessorSymbol: IrSimpleFunctionSymbol
    ): IrCall {
        val call = IrCallImpl(
            oldExpression.startOffset, oldExpression.endOffset,
            oldExpression.type,
            accessorSymbol, 0, accessorSymbol.owner.valueParameters.size,
            oldExpression.origin
        )
        oldExpression.receiver?.let {
            call.putValueArgument(0, oldExpression.receiver)
        }
        call.putValueArgument(call.valueArgumentsCount - 1, oldExpression.value)
        return call
    }

    private fun copyAllParamsToArgs(
        call: IrFunctionAccessExpression,
        syntheticFunction: IrFunction
    ) {
        var typeArgumentOffset = 0
        if (syntheticFunction is IrConstructor) {
            call.passTypeArgumentsFrom(syntheticFunction.parentAsClass)
            typeArgumentOffset = syntheticFunction.parentAsClass.typeParameters.size
        }
        call.passTypeArgumentsFrom(syntheticFunction, offset = typeArgumentOffset)

        var offset = 0
        val delegateTo = call.symbol.owner
        delegateTo.dispatchReceiverParameter?.let {
            call.dispatchReceiver =
                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, syntheticFunction.valueParameters[offset++].symbol)
        }

        delegateTo.extensionReceiverParameter?.let {
            call.extensionReceiver =
                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, syntheticFunction.valueParameters[offset++].symbol)
        }

        delegateTo.valueParameters.forEachIndexed { i, _ ->
            call.putValueArgument(
                i,
                IrGetValueImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    syntheticFunction.valueParameters[i + offset].symbol
                )
            )
        }
    }

    private fun IrSimpleFunction.accessorName(superQualifier: IrClassSymbol?): Name {
        val jvmName = context.methodSignatureMapper.mapFunctionName(this)
        val suffix = when {
            // Accessors for top level functions never need a suffix.
            isTopLevel -> ""

            // The only function accessors placed on interfaces are for private functions and JvmDefault implementations.
            // The two cannot clash.
            parentAsClass.isJvmInterface -> if (!DescriptorVisibilities.isPrivate(visibility)) "\$jd" else ""

            // Accessor for _s_uper-qualified call
            superQualifier != null -> "\$s" + superQualifier.owner.syntheticAccessorToSuperSuffix()

            // Access to protected members that need an accessor must be because they are inherited,
            // hence accessed on a _s_upertype. If what is accessed is static, we can point to different
            // parts of the inheritance hierarchy and need to distinguish with a suffix.
            isStatic && visibility.isProtected -> "\$s" + parentAsClass.syntheticAccessorToSuperSuffix()

            else -> ""
        }
        return Name.identifier("access\$$jvmName$suffix")
    }

    private fun IrField.accessorNameForGetter(superQualifierSymbol: IrClassSymbol?): Name {
        val getterName = JvmAbi.getterName(name.asString())
        return Name.identifier("access\$$getterName\$${fieldAccessorSuffix(superQualifierSymbol)}")
    }

    private fun IrField.accessorNameForSetter(superQualifierSymbol: IrClassSymbol?): Name {
        val setterName = JvmAbi.setterName(name.asString())
        return Name.identifier("access\$$setterName\$${fieldAccessorSuffix(superQualifierSymbol)}")
    }

    private fun IrField.fieldAccessorSuffix(superQualifierSymbol: IrClassSymbol?): String {
        // Special _c_ompanion _p_roperty suffix for accessing companion backing field moved to outer
        if (origin == JvmLoweredDeclarationOrigin.COMPANION_PROPERTY_BACKING_FIELD && !parentAsClass.isCompanion) {
            return "cp"
        }

        if (superQualifierSymbol != null) {
            return "p\$s${superQualifierSymbol.owner.syntheticAccessorToSuperSuffix()}"
        }

        // Accesses to static protected fields that need an accessor must be due to being inherited, hence accessed on a
        // _s_upertype. If the field is static, the super class the access is on can be different and therefore
        // we generate a suffix to distinguish access to field with different receiver types in the super hierarchy.
        return "p" + if (isStatic && visibility.isProtected) "\$s" + parentAsClass.syntheticAccessorToSuperSuffix() else ""
    }

    private val DescriptorVisibility.isPrivate
        get() = DescriptorVisibilities.isPrivate(this)

    private val DescriptorVisibility.isProtected
        get() = this == DescriptorVisibilities.PROTECTED ||
                this == JavaDescriptorVisibilities.PROTECTED_AND_PACKAGE ||
                this == JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY

    private fun IrSymbol.isAccessible(withSuper: Boolean, thisObjReference: IrClassSymbol?): Boolean {
        /// We assume that IR code that reaches us has been checked for correctness at the frontend.
        /// This function needs to single out those cases where Java accessibility rules differ from Kotlin's.

        val symbolOwner = owner
        val declarationRaw = symbolOwner as IrDeclarationWithVisibility
        val declaration =
            (declarationRaw as? IrSimpleFunction)?.resolveFakeOverride(allowAbstract = true) ?: declarationRaw

        // There is never a problem with visibility of inline functions, as those don't end up as Java entities
        if (declaration is IrFunction && declaration.isInline) return true

        // Enum entry constructors are generated as package-private and are accessed only from corresponding enum class
        if (declaration is IrConstructor && declaration.constructedClass.isEnumEntry) return true

        // `internal` maps to public and requires no accessor.
        if (!withSuper && !declaration.visibility.isPrivate && !declaration.visibility.isProtected) return true

        // `toArray` is always accessible cause mapped to public functions
        if (symbolOwner is IrSimpleFunction && (symbolOwner.isNonGenericToArray() || symbolOwner.isGenericToArray(context))) {
            if (symbolOwner.parentAsClass.isCollectionSubClass) {
                return true
            }
        }

        // EnumEntry constructors are always accessible (they are only called from the enclosing Enum class)
        if (symbolOwner is IrConstructor && symbolOwner.parentClassOrNull?.isEnumEntry == true)
            return true

        // If local variables are accessible by Kotlin rules, they also are by Java rules.
        val ownerClass = declaration.parent as? IrClass ?: return true

        var context = currentScope!!.irElement as IrDeclaration
        var throughCrossinlineLambda = false
        while (context !is IrClass) {
            val callSite = inlineLambdaToCallSite[context]
            if (callSite != null) {
                // For inline lambdas, we can navigate to the only call site directly. Crossinline lambdas might be inlined
                // into other classes in the same package, so private/super require accessors anyway.
                throughCrossinlineLambda = throughCrossinlineLambda || callSite.crossinline
                context = callSite.scope
            } else if (context is IrFunction && context.isInline) {
                // Accesses from inline functions can actually be anywhere; even private inline functions can be
                // inlined into a different class, e.g. a callable reference. For protected inline functions
                // calling methods on `super` we also need an accessor to satisfy INVOKESPECIAL constraints.
                // TODO scan nested classes for calls to private inline functions?
                return false
            } else {
                context = context.parent as? IrDeclaration ?: return false
            }
        }

        val samePackage = ownerClass.getPackageFragment()?.fqName == context.getPackageFragment()?.fqName
        val fromSubclassOfReceiversClass = !throughCrossinlineLambda &&
                context.isSubclassOf(ownerClass) && (thisObjReference == null || context.symbol.isSubtypeOfClass(thisObjReference))
        return when {
            // private suspend functions are generated as synthetic package private
            declaration is IrFunction && declaration.isSuspend && declaration.visibility.isPrivate && samePackage -> true
            declaration.visibility.isPrivate && (throughCrossinlineLambda || ownerClass != context) -> false
            declaration.visibility.isProtected && !samePackage && !fromSubclassOfReceiversClass -> false
            withSuper && !fromSubclassOfReceiversClass -> false
            else -> true
        }
    }

    // monitorEnter/monitorExit are the only functions which are accessed "illegally" (see kotlin/util/Synchronized.kt).
    // Since they are intrinsified in the codegen, SyntheticAccessorLowering should not crash on attempt to add accessors for them.
    private fun IrFunction.isAllowedToBeAddedToForeignFile(): Boolean =
        (name.asString() == "access\$monitorEnter" || name.asString() == "access\$monitorExit") &&
                context.irIntrinsics.getIntrinsic(symbol) != null
}

private fun IrFunction.isCoroutineIntrinsic(): Boolean =
    (name.asString() == "suspendCoroutine" && getPackageFragment()?.fqName == FqName("kotlin.coroutines")) ||
            (name.asString() == "suspendCoroutineUninterceptedOrReturn" && getPackageFragment()?.fqName == FqName("kotlin.coroutines.intrinsics"))

private fun IrClass.syntheticAccessorToSuperSuffix(): String =
    // TODO: change this to `fqNameUnsafe.asString().replace(".", "_")` as soon as we're ready to break compatibility with pre-KT-21178 code
    name.asString().hashCode().toString()


