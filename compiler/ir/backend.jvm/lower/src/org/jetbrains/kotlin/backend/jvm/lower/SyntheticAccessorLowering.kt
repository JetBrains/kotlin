/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.hasMangledParameters
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.codegen.AsmUtil
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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.org.objectweb.asm.Opcodes

internal class SyntheticAccessorLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val pendingAccessorsToAdd = mutableListOf<IrFunction>()
        irFile.transformChildrenVoid(SyntheticAccessorTransformer(context, irFile.findInlineCallSites(context), pendingAccessorsToAdd))
        for (accessor in pendingAccessorsToAdd) {
            assert(accessor.fileOrNull == irFile) {
                "SyntheticAccessorLowering should not attempt to modify other files!\n" +
                        "While lowering this file: ${irFile.render()}\n" +
                        "Trying to add this accessor: ${accessor.render()}"
            }
            (accessor.parent as IrDeclarationContainer).declarations.add(accessor)
        }
    }

    companion object {
        fun IrSymbol.isAccessible(
            context: JvmBackendContext,
            currentScope: ScopeWithIr?,
            inlineScopeResolver: IrInlineScopeResolver,
            withSuper: Boolean, thisObjReference: IrClassSymbol?,
        ): Boolean {
            /// We assume that IR code that reaches us has been checked for correctness at the frontend.
            /// This function needs to single out those cases where Java accessibility rules differ from Kotlin's.
            val declarationRaw = owner as IrDeclarationWithVisibility

            // If this expression won't actually result in a JVM instruction call, access modifiers don't matter.
            if (declarationRaw is IrFunction && (declarationRaw.isInline || context.getIntrinsic(declarationRaw.symbol) != null))
                return true

            // Enum entry constructors are generated as package-private and are accessed only from corresponding enum class
            if (declarationRaw is IrConstructor && declarationRaw.constructedClass.isEnumEntry) return true

            // Public declarations are already accessible. However, `super` calls are subclass-only.
            val jvmVisibility = AsmUtil.getVisibilityAccessFlag(declarationRaw.visibility.delegate)
            if (jvmVisibility == Opcodes.ACC_PUBLIC && !withSuper) return true

            // `toArray` is always accessible cause mapped to public functions
            if (declarationRaw is IrSimpleFunction && (declarationRaw.isNonGenericToArray() || declarationRaw.isGenericToArray(context)) &&
                declarationRaw.parentAsClass.isCollectionSubClass
            ) return true

            // `$assertionsDisabled` is accessed only from the same class, even in an inline function
            // (the inliner will generate it at the call site if necessary).
            if (declarationRaw is IrField && declarationRaw.isAssertionsDisabledField(context)) return true

            val declaration = when (declarationRaw) {
                is IrSimpleFunction -> declarationRaw.resolveFakeOverride(allowAbstract = true)!!
                is IrField -> declarationRaw.resolveFakeOverride()
                else -> declarationRaw
            }

            val ownerClass = declaration.parent as? IrClass ?: return true // locals are always accessible
            val scopeClassOrPackage = inlineScopeResolver.findContainer(currentScope!!.irElement) ?: return false
            val samePackage = ownerClass.getPackageFragment()?.fqName == scopeClassOrPackage.getPackageFragment()?.fqName
            return when {
                jvmVisibility == 0 /* package only */ -> samePackage
                jvmVisibility == Opcodes.ACC_PRIVATE -> ownerClass == scopeClassOrPackage
                // JVM `protected`, unlike Kotlin `protected`, permits accesses from the same package.
                !withSuper && samePackage -> true
                // Super calls and cross-package protected accesses are both only possible from a subclass of the declaration
                // owner. Also, the target of a non-static call must be assignable to the current class. This is a verification
                // constraint: https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.10.1.8
                else -> (scopeClassOrPackage is IrClass && scopeClassOrPackage.isSubclassOf(ownerClass)) &&
                        (thisObjReference == null || thisObjReference.owner.isSubclassOf(scopeClassOrPackage))
            }
        }
    }
}

private class SyntheticAccessorTransformer(
    val context: JvmBackendContext,
    val inlineScopeResolver: IrInlineScopeResolver,
    val pendingAccessorsToAdd: MutableList<IrFunction>
) : IrElementTransformerVoidWithContext() {

    private data class FieldKey(val fieldSymbol: IrFieldSymbol, val parent: IrDeclarationParent, val superQualifierSymbol: IrClassSymbol?)

    private data class FunctionKey(
        val functionSymbol: IrFunctionSymbol,
        val parent: IrDeclarationParent,
        val superQualifierSymbol: IrClassSymbol?
    )

    private val functionMap = mutableMapOf<FunctionKey, IrFunctionSymbol>()
    private val getterMap = mutableMapOf<FieldKey, IrSimpleFunctionSymbol>()
    private val setterMap = mutableMapOf<FieldKey, IrSimpleFunctionSymbol>()

    private fun IrSymbol.isAccessible(withSuper: Boolean, thisObjReference: IrClassSymbol?): Boolean =
        with(SyntheticAccessorLowering) {
            isAccessible(context, currentScope, inlineScopeResolver, withSuper, thisObjReference)
        }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        if (expression.usesDefaultArguments()) {
            return super.visitFunctionAccess(expression)
        }

        val callee = expression.symbol.owner
        val withSuper = (expression as? IrCall)?.superQualifierSymbol != null
        val thisSymbol = (expression as? IrCall)?.dispatchReceiver?.type?.classifierOrNull as? IrClassSymbol

        if (expression is IrCall && callee.symbol == context.ir.symbols.indyLambdaMetafactoryIntrinsic) {
            return super.visitExpression(handleLambdaMetafactoryIntrinsic(expression, thisSymbol))
        }

        val accessor = when {
            callee is IrConstructor && callee.isOrShouldBeHidden ->
                handleHiddenConstructor(callee).symbol
            !expression.symbol.isAccessible(withSuper, thisSymbol) ->
                createAccessor(expression)
            else ->
                return super.visitFunctionAccess(expression)
        }
        return super.visitExpression(modifyFunctionAccessExpression(expression, accessor))
    }

    private fun createAccessor(expression: IrFunctionAccessExpression): IrFunctionSymbol =
        if (expression is IrCall)
            createAccessor(expression.symbol, expression.dispatchReceiver?.type, expression.superQualifierSymbol)
        else
            createAccessor(expression.symbol, null, null)

    private fun createAccessor(
        symbol: IrFunctionSymbol,
        dispatchReceiverType: IrType?,
        superQualifierSymbol: IrClassSymbol?
    ): IrFunctionSymbol {
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
        return functionMap.getOrPut(FunctionKey(symbol, parent, superQualifierSymbol)) {
            when (symbol) {
                is IrConstructorSymbol ->
                    symbol.owner.makeConstructorAccessor()
                        .also(pendingAccessorsToAdd::add)
                        .symbol
                is IrSimpleFunctionSymbol -> {
                    symbol.owner.makeSimpleFunctionAccessor(superQualifierSymbol, dispatchReceiverType, parent)
                        .also(pendingAccessorsToAdd::add)
                        .symbol
                }
                else -> error("Unknown subclass of IrFunctionSymbol")
            }
        }
    }

    private fun handleLambdaMetafactoryIntrinsic(call: IrCall, thisSymbol: IrClassSymbol?): IrExpression {
        val implFunRef = call.getValueArgument(1) as? IrFunctionReference
            ?: throw AssertionError("'implMethodReference' is expected to be 'IrFunctionReference': ${call.dump()}")
        val implFunSymbol = implFunRef.symbol

        if (implFunSymbol.isAccessibleFromSyntheticProxy(thisSymbol))
            return call

        val accessorSymbol = createAccessor(implFunSymbol, implFunRef.dispatchReceiver?.type, null)
        val accessorFun = accessorSymbol.owner
        val accessorRef =
            IrFunctionReferenceImpl(
                implFunRef.startOffset, implFunRef.endOffset, implFunRef.type,
                accessorSymbol,
                accessorFun.typeParameters.size,
                accessorFun.valueParameters.size,
                implFunRef.reflectionTarget, implFunRef.origin
            )

        accessorRef.copyTypeArgumentsFrom(implFunRef)

        val implFun = implFunSymbol.owner
        var accessorArgIndex = 0
        if (implFun.dispatchReceiverParameter != null) {
            accessorRef.putValueArgument(accessorArgIndex++, implFunRef.dispatchReceiver)
        }
        if (implFun.extensionReceiverParameter != null) {
            accessorRef.putValueArgument(accessorArgIndex++, implFunRef.extensionReceiver)
        }
        for (implArgIndex in 0 until implFunRef.valueArgumentsCount) {
            accessorRef.putValueArgument(accessorArgIndex++, implFunRef.getValueArgument(implArgIndex))
        }
        if (accessorFun is IrConstructor) {
            accessorRef.putValueArgument(accessorArgIndex, createAccessorMarkerArgument())
        }

        call.putValueArgument(1, accessorRef)
        return call
    }

    private fun IrFunctionSymbol.isAccessibleFromSyntheticProxy(thisSymbol: IrClassSymbol?): Boolean {
        if (!isAccessible(false, thisSymbol))
            return false

        if (owner.visibility != DescriptorVisibilities.PROTECTED &&
            owner.visibility != JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY
        ) {
            return true
        }

        // We have a protected member.
        // It is accessible from a synthetic proxy class (created by LambdaMetafactory)
        // if it belongs to the current class.
        return inlineScopeResolver.findContainer(currentScope!!.irElement) == owner.parentAsClass
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        val dispatchReceiverType = expression.receiver?.type
        val dispatchReceiverClassSymbol = dispatchReceiverType?.classifierOrNull as? IrClassSymbol
        return super.visitExpression(
            if (!expression.symbol.isAccessible(false, dispatchReceiverClassSymbol)) {
                val symbol = expression.symbol
                val parent = symbol.owner.accessorParent(dispatchReceiverClassSymbol?.owner ?: symbol.owner.parent) as IrClass
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
        // FE accepts code that assigns to a val of this or other class if it happens in unreachable code (KT-35565).
        // Sometimes this can cause internal error in the BE (see KT-49316).
        // Assume that 'val' property with a backing field can never be initialized from a context that requires synthetic accessor.
        val correspondingProperty = expression.symbol.owner.correspondingPropertySymbol?.owner
        if (correspondingProperty != null && !correspondingProperty.isVar) {
            return super.visitExpression(expression)
        }

        val dispatchReceiverType = expression.receiver?.type
        val dispatchReceiverClassSymbol = dispatchReceiverType?.classifierOrNull as? IrClassSymbol
        if (expression.symbol.isAccessible(false, dispatchReceiverClassSymbol)) {
            return super.visitExpression(expression)
        }

        val symbol = expression.symbol
        val parent = symbol.owner.accessorParent(dispatchReceiverClassSymbol?.owner ?: symbol.owner.parent) as IrClass

        return super.visitExpression(
            modifySetterExpression(
                expression,
                setterMap.getOrPut(FieldKey(symbol, parent, expression.superQualifierSymbol)) {
                    makeSetterAccessorSymbol(symbol, parent, expression.superQualifierSymbol)
                }
            )
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
            if (this in context.hiddenConstructors.keys)
                return true

            if (origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
                origin == JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR ||
                origin == JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR_FOR_HIDDEN_CONSTRUCTOR
            ) {
                return false
            }

            val constructedClass = constructedClass

            if (!DescriptorVisibilities.isPrivate(visibility) && !constructedClass.isInline && hasMangledParameters &&
                !constructedClass.isAnonymousObject
            ) return true

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
        } else {
            parent
        }

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

    private fun IrSimpleFunction.makeSimpleFunctionAccessor(
        superQualifierSymbol: IrClassSymbol?,
        dispatchReceiverType: IrType?,
        parent: IrDeclarationParent
    ): IrSimpleFunction {
        val source = this

        return factory.buildFun {
            startOffset = parent.startOffset
            endOffset = parent.startOffset
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = source.accessorName(superQualifierSymbol)
            visibility = DescriptorVisibilities.PUBLIC
            modality = if (parent is IrClass && parent.isJvmInterface) Modality.OPEN else Modality.FINAL
            isSuspend = source.isSuspend // synthetic accessors of suspend functions are handled in codegen
        }.also { accessor ->
            accessor.parent = parent
            accessor.copyAttributes(source)
            accessor.copyTypeParametersFrom(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.copyValueParametersToStatic(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR, dispatchReceiverType)
            accessor.returnType = source.returnType.remapTypeParameters(source, accessor)

            accessor.body = IrExpressionBodyImpl(
                accessor.startOffset, accessor.startOffset,
                createSimpleFunctionCall(accessor, source.symbol, superQualifierSymbol)
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
            newExpression.putValueArgument(receiverAndArgs.size, createAccessorMarkerArgument())
        }
        return newExpression
    }

    private fun createAccessorMarkerArgument() =
        IrConstImpl.constNull(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.ir.symbols.defaultConstructorMarker.defaultType.makeNullable())

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
            currentClass?.irElement?.let { element ->
                element is IrClass && element.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS && element.parentAsClass == parentAsClass
            } ?: false -> if (!DescriptorVisibilities.isPrivate(visibility)) "\$jd" else ""

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
}

private fun IrClass.syntheticAccessorToSuperSuffix(): String =
    // TODO: change this to `fqNameUnsafe.asString().replace(".", "_")` as soon as we're ready to break compatibility with pre-KT-21178 code
    name.asString().hashCode().toString()

private fun IrField.resolveFakeOverride(): IrField {
    val correspondingProperty = correspondingPropertySymbol?.owner
    if (correspondingProperty == null || !correspondingProperty.isFakeOverride)
        return this
    val realProperty = correspondingProperty.resolveFakeOverride()
        ?: throw AssertionError("No real override for ${correspondingProperty.render()}")
    return realProperty.backingField
        ?: throw AssertionError(
            "Fake override property ${correspondingProperty.render()} with backing field " +
                    "overrides a real property with no backing field: ${realProperty.render()}"
        )
}

private val DescriptorVisibility.isProtected
    get() = AsmUtil.getVisibilityAccessFlag(delegate) == Opcodes.ACC_PROTECTED
