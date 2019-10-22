/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.copyValueParametersToStatic
import org.jetbrains.kotlin.backend.common.ir.passTypeArgumentsFrom
import org.jetbrains.kotlin.backend.common.ir.remapTypeParameters
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.intrinsics.receiverAndArgs
import org.jetbrains.kotlin.backend.jvm.ir.IrInlineReferenceLocator
import org.jetbrains.kotlin.backend.jvm.ir.isLambda
import org.jetbrains.kotlin.backend.jvm.ir.shouldBeHidden
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class SyntheticAccessorLowering(val context: JvmBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {
    private val pendingTransformations = mutableListOf<Function0<Unit>>()
    private val inlineLambdaToCallSite = mutableMapOf<IrFunction, IrDeclaration?>()

    override fun lower(irFile: IrFile) {
        inlineLambdaToCallSite.putAll(IrInlineReferenceLocator.scan(context, irFile).lambdaToCallSite)
        irFile.transformChildrenVoid(this)
        pendingTransformations.forEach { it() }
    }

    private val functionMap = mutableMapOf<IrFunctionSymbol, IrFunctionSymbol>()
    private val getterMap = mutableMapOf<IrFieldSymbol, IrSimpleFunctionSymbol>()
    private val setterMap = mutableMapOf<IrFieldSymbol, IrSimpleFunctionSymbol>()

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        if (expression.usesDefaultArguments()) {
            return super.visitFunctionAccess(expression)
        }

        val withSuper = (expression as? IrCall)?.superQualifierSymbol != null
        val thisSymbol = (expression as? IrCall)?.dispatchReceiver?.type?.classifierOrNull as? IrClassSymbol
        if (expression.symbol.isAccessible(withSuper, thisSymbol)) {
            return super.visitFunctionAccess(expression)
        }
        return super.visitExpression(
            modifyFunctionAccessExpression(expression, functionMap.getOrPut(expression.symbol) {
                when (val symbol = expression.symbol) {
                    is IrConstructorSymbol -> symbol.owner.makeConstructorAccessor().symbol
                    is IrSimpleFunctionSymbol -> symbol.owner.makeSimpleFunctionAccessor(expression as IrCall).symbol
                    else -> error("Unknown subclass of IrFunctionSymbol")
                }
            })
        )
    }

    override fun visitGetField(expression: IrGetField) = super.visitExpression(
        if (!expression.symbol.isAccessible(false, expression.receiver?.type?.classifierOrNull as? IrClassSymbol)) {
            modifyGetterExpression(expression, getterMap.getOrPut(expression.symbol) { makeGetterAccessorSymbol(expression.symbol) })
        } else {
            expression
        }
    )

    override fun visitSetField(expression: IrSetField) = super.visitExpression(
        if (!expression.symbol.isAccessible(false, expression.receiver?.type?.classifierOrNull as? IrClassSymbol)) {
            modifySetterExpression(expression, setterMap.getOrPut(expression.symbol) { makeSetterAccessorSymbol(expression.symbol) })
        } else {
            expression
        }
    )

    override fun visitConstructor(declaration: IrConstructor): IrStatement {
        handleHiddenConstructor(declaration)
        return super.visitConstructor(declaration)
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        handleHiddenConstructor(expression.symbol.owner)
        return super.visitConstructorCall(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val function = expression.symbol.owner

        if (!expression.origin.isLambda && function is IrConstructor) {
            handleHiddenConstructor(function)?.let { accessor ->
                expression.transformChildrenVoid()
                return IrFunctionReferenceImpl(
                    expression.startOffset, expression.endOffset, expression.type,
                    accessor, accessor.owner.typeParameters.size,
                    accessor.owner.valueParameters.size, expression.origin
                )
            }
        }

        return super.visitFunctionReference(expression)
    }

    private fun handleHiddenConstructor(declaration: IrConstructor): IrConstructorSymbol? {
        functionMap[declaration.symbol]?.let { return it as IrConstructorSymbol }

        if (!declaration.shouldBeHidden)
            return null

        declaration.visibility = Visibilities.PRIVATE

        return declaration.makeConstructorAccessor().also { accessor ->
            functionMap[declaration.symbol] = accessor.symbol

            // There's a special case in the JVM backend for serializing the metadata of hidden
            // constructors - we serialize the descriptor of the original constructor, but the
            // signature of the bridge. We implement this special case in the JVM IR backend by
            // attaching the metadata directly to the bridge. We also have to move all annotations
            // to the bridge method. Parameter annotations are already moved by the copyTo method.
            accessor.metadata = declaration.metadata
            declaration.safeAs<IrConstructorImpl>()?.metadata = null
            accessor.annotations += declaration.annotations
            declaration.annotations.clear()
            declaration.valueParameters.forEach { it.annotations.clear() }
        }.symbol
    }

    // In case of Java `protected static`, access could be done from a public inline function in the same package,
    // or a subclass of the Java class. Both cases require an accessor, which we cannot add to the Java class.
    private fun IrDeclarationWithVisibility.accessorParent(parent: IrDeclarationParent = this.parent) =
        if (visibility == JavaVisibilities.PROTECTED_STATIC_VISIBILITY) {
            val classes = allScopes.map { it.irElement }.filterIsInstance<IrClass>()
            classes.lastOrNull { parent is IrClass && it.isSubclassOf(parent) } ?: classes.last()
        } else parent

    private fun IrConstructor.makeConstructorAccessor(): IrConstructorImpl {
        val source = this

        return buildConstructor {
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = source.name
            visibility = Visibilities.PUBLIC

        }.also { accessor ->
            accessor.parent = source.parent
            pendingTransformations.add { (accessor.parent as IrDeclarationContainer).declarations.add(accessor) }

            accessor.copyTypeParametersFrom(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.copyValueParametersToStatic(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.returnType = source.returnType.remapTypeParameters(source, accessor)

            accessor.addValueParameter(
                "marker", context.ir.symbols.defaultConstructorMarker.owner.defaultType, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
            )

            accessor.body = IrExpressionBodyImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                createConstructorCall(accessor, source.symbol)
            )
        }
    }

    private fun createConstructorCall(accessor: IrConstructor, targetSymbol: IrConstructorSymbol) =
        IrDelegatingConstructorCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            context.irBuiltIns.unitType,
            targetSymbol, targetSymbol.owner.parentAsClass.typeParameters.size + targetSymbol.owner.typeParameters.size
        ).also {
            copyAllParamsToArgs(it, accessor)
        }

    private fun IrSimpleFunction.makeSimpleFunctionAccessor(expression: IrCall): IrSimpleFunction {
        val source = this
        return buildFun {
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = source.accessorName()
            visibility = Visibilities.PUBLIC
            isSuspend = source.isSuspend // synthetic accessors of suspend functions are handled in codegen
        }.also { accessor ->
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
            val dispatchReceiverType = expression.dispatchReceiver?.type
            accessor.parent = source.accessorParent(dispatchReceiverType?.classOrNull?.owner ?: source.parent)
            pendingTransformations.add { (accessor.parent as IrDeclarationContainer).declarations.add(accessor) }

            accessor.copyTypeParametersFrom(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.copyValueParametersToStatic(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR, dispatchReceiverType)
            accessor.returnType = source.returnType.remapTypeParameters(source, accessor)

            accessor.body = IrExpressionBodyImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                createSimpleFunctionCall(accessor, source.symbol, expression.superQualifierSymbol)
            )
        }
    }

    private fun createSimpleFunctionCall(accessor: IrFunction, targetSymbol: IrFunctionSymbol, superQualifierSymbol: IrClassSymbol?) =
        IrCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            accessor.returnType,
            targetSymbol, targetSymbol.owner.typeParameters.size,
            superQualifierSymbol = superQualifierSymbol
        ).also {
            copyAllParamsToArgs(it, accessor)
        }

    private fun makeGetterAccessorSymbol(fieldSymbol: IrFieldSymbol): IrSimpleFunctionSymbol =
        buildFun {
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = fieldSymbol.owner.accessorNameForGetter()
            visibility = Visibilities.PUBLIC
            modality = Modality.FINAL
            returnType = fieldSymbol.owner.type
        }.also { accessor ->
            accessor.parent = fieldSymbol.owner.accessorParent()
            pendingTransformations.add { (accessor.parent as IrDeclarationContainer).declarations.add(accessor) }

            if (!fieldSymbol.owner.isStatic) {
                accessor.addValueParameter(
                    "\$this", fieldSymbol.owner.parentAsClass.defaultType, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
                )
            }

            accessor.body = createAccessorBodyForGetter(fieldSymbol.owner, accessor)
        }.symbol

    private fun createAccessorBodyForGetter(targetField: IrField, accessor: IrSimpleFunction): IrBody {
        val resolvedTargetField = targetField.resolveFakeOverride()!!
        val maybeDispatchReceiver =
            if (resolvedTargetField.isStatic) null
            else IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, accessor.valueParameters[0].symbol)
        return IrExpressionBodyImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            IrGetFieldImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                resolvedTargetField.symbol,
                resolvedTargetField.type,
                maybeDispatchReceiver
            )
        )
    }

    private fun makeSetterAccessorSymbol(fieldSymbol: IrFieldSymbol): IrSimpleFunctionSymbol =
        buildFun {
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = fieldSymbol.owner.accessorNameForSetter()
            visibility = Visibilities.PUBLIC
            modality = Modality.FINAL
            returnType = context.irBuiltIns.unitType
        }.also { accessor ->
            accessor.parent = fieldSymbol.owner.accessorParent()
            pendingTransformations.add { (accessor.parent as IrDeclarationContainer).declarations.add(accessor) }

            if (!fieldSymbol.owner.isStatic) {
                accessor.addValueParameter(
                    "\$this", fieldSymbol.owner.parentAsClass.defaultType, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
                )
            }

            accessor.addValueParameter("value", fieldSymbol.owner.type, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)

            accessor.body = createAccessorBodyForSetter(fieldSymbol.owner, accessor)
        }.symbol

    private fun createAccessorBodyForSetter(targetField: IrField, accessor: IrSimpleFunction): IrBody {
        val resolvedTargetField = targetField.resolveFakeOverride()!!
        val maybeDispatchReceiver =
            if (resolvedTargetField.isStatic) null
            else IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, accessor.valueParameters[0].symbol)
        val value = IrGetValueImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            accessor.valueParameters[if (resolvedTargetField.isStatic) 0 else 1].symbol
        )
        return IrExpressionBodyImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            IrSetFieldImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                resolvedTargetField.symbol,
                maybeDispatchReceiver,
                value,
                context.irBuiltIns.unitType
            )
        )
    }

    private fun modifyFunctionAccessExpression(
        oldExpression: IrFunctionAccessExpression,
        accessorSymbol: IrFunctionSymbol
    ): IrFunctionAccessExpression {
        val newExpression = when (oldExpression) {
            is IrCall -> IrCallImpl(
                oldExpression.startOffset, oldExpression.endOffset,
                oldExpression.type,
                accessorSymbol, oldExpression.typeArgumentsCount,
                oldExpression.origin
            )
            is IrDelegatingConstructorCall -> IrDelegatingConstructorCallImpl(
                oldExpression.startOffset, oldExpression.endOffset,
                context.irBuiltIns.unitType,
                accessorSymbol as IrConstructorSymbol, oldExpression.typeArgumentsCount
            )
            is IrConstructorCall ->
                IrConstructorCallImpl.fromSymbolDescriptor(
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
                    context.ir.symbols.defaultConstructorMarker.owner.defaultType.makeNullable()
                )
            )
        }
        return newExpression
    }

    private fun modifyGetterExpression(
        oldExpression: IrGetField,
        accessorSymbol: IrFunctionSymbol
    ): IrCall {
        val call = IrCallImpl(
            oldExpression.startOffset, oldExpression.endOffset,
            oldExpression.type,
            accessorSymbol, 0,
            oldExpression.origin
        )
        oldExpression.receiver?.let {
            call.putValueArgument(0, oldExpression.receiver)
        }
        return call
    }

    private fun modifySetterExpression(
        oldExpression: IrSetField,
        accessorSymbol: IrFunctionSymbol
    ): IrCall {
        val call = IrCallImpl(
            oldExpression.startOffset, oldExpression.endOffset,
            oldExpression.type,
            accessorSymbol, 0,
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

    // Counter used to disambiguate accessors to functions with the same base name.
    private var nameCounter = 0

    private fun IrFunction.accessorName(): Name {
        val jvmName = context.methodSignatureMapper.mapFunctionName(this)
        return Name.identifier("access\$$jvmName\$${nameCounter++}")
    }

    private fun IrField.accessorNameForGetter(): Name {
        val getterName = JvmAbi.getterName(name.asString())
        return Name.identifier("access\$prop\$$getterName\$${nameCounter++}")
    }

    private fun IrField.accessorNameForSetter(): Name {
        val setterName = JvmAbi.setterName(name.asString())
        return Name.identifier("access\$prop\$$setterName\$${nameCounter++}")
    }

    private val Visibility.isPrivate
        get() = Visibilities.isPrivate(this)

    private val Visibility.isProtected
        get() = this == Visibilities.PROTECTED ||
                this == JavaVisibilities.PROTECTED_AND_PACKAGE ||
                this == JavaVisibilities.PROTECTED_STATIC_VISIBILITY

    private fun IrDeclaration.getAccessContext(withSuper: Boolean): IrDeclarationContainer? = when {
        this is IrDeclarationContainer -> this
        // For inline lambdas, we can navigate to the only call site directly.
        this in inlineLambdaToCallSite -> inlineLambdaToCallSite[this]?.getAccessContext(withSuper)
        // Accesses from inline functions can actually be anywhere; even private inline functions can be
        // inlined into a different class, e.g. a callable reference. For protected inline functions
        // calling methods on `super` we also need an accessor to satisfy INVOKESPECIAL constraints.
        // TODO scan nested classes for calls to private inline functions?
        this is IrFunction && isInline -> null
        else -> (parent as? IrDeclaration)?.getAccessContext(withSuper)
    }

    private fun IrSymbol.isAccessible(withSuper: Boolean, thisObjReference: IrClassSymbol?): Boolean {
        /// We assume that IR code that reaches us has been checked for correctness at the frontend.
        /// This function needs to single out those cases where Java accessibility rules differ from Kotlin's.

        val declarationRaw = owner as IrDeclarationWithVisibility
        val declaration =
            (declarationRaw as? IrSimpleFunction)?.resolveFakeOverride()
                ?: (declarationRaw as? IrField)?.resolveFakeOverride() ?: declarationRaw

        // There is never a problem with visibility of inline functions, as those don't end up as Java entities
        if (declaration is IrFunction && declaration.isInline) return true

        // `internal` maps to public and requires no accessor.
        if (!withSuper && !declaration.visibility.isPrivate && !declaration.visibility.isProtected) return true

        // If local variables are accessible by Kotlin rules, they also are by Java rules.
        val symbolDeclarationContainer = (declaration.parent as? IrDeclarationContainer) as? IrElement ?: return true
        val contextDeclarationContainer = (currentScope!!.irElement as IrDeclaration).getAccessContext(withSuper) ?: return false

        val samePackage = declaration.getPackageFragment()?.fqName == contextDeclarationContainer.getPackageFragment()?.fqName
        val fromSubclassOfReceiversClass = contextDeclarationContainer is IrClass && symbolDeclarationContainer is IrClass &&
                ((thisObjReference == null || contextDeclarationContainer.symbol.isSubtypeOfClass(thisObjReference)) &&
                        (contextDeclarationContainer.isSubclassOf(symbolDeclarationContainer)))
        return when {
            declaration.visibility.isPrivate && symbolDeclarationContainer != contextDeclarationContainer -> false
            declaration.visibility.isProtected && !samePackage && !fromSubclassOfReceiversClass -> false
            withSuper && !fromSubclassOfReceiversClass -> false
            else -> true
        }
    }
}
