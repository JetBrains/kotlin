/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.intrinsics.receiverAndArgs
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils

class SyntheticAccessorLowering(val context: JvmBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {
    private val pendingTransformations = mutableListOf<Function0<Unit>>()
    private val inlinedLambdasCollector = InlinedLambdasCollector()

    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(inlinedLambdasCollector)
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
        return super.visitExpression(
            handleAccess(expression, expression.symbol, functionMap, ::makeFunctionAccessorSymbol, ::modifyFunctionAccessExpression)
        )
    }

    override fun visitGetField(expression: IrGetField) = super.visitExpression(
        handleAccess(expression, expression.symbol, getterMap, ::makeGetterAccessorSymbol, ::modifyGetterExpression)
    )

    override fun visitSetField(expression: IrSetField) = super.visitExpression(
        handleAccess(expression, expression.symbol, setterMap, ::makeSetterAccessorSymbol, ::modifySetterExpression)
    )

    private inline fun <ExprT : IrDeclarationReference, reified FromSyT : IrSymbol, ToSyT : IrSymbol> handleAccess(
        expression: ExprT,
        symbol: FromSyT,
        accumMap: MutableMap<FromSyT, ToSyT>,
        symbolConverter: (FromSyT) -> ToSyT,
        exprConverter: (ExprT, ToSyT) -> IrDeclarationReference
    ): IrExpression {
        if (!symbol.isAccessible()) {
            val accessorSymbol = accumMap.getOrPut(symbol, { symbolConverter(symbol) })
            return exprConverter(expression, accessorSymbol)
        } else {
            return expression
        }
    }

    private fun makeFunctionAccessorSymbol(functionSymbol: IrFunctionSymbol): IrFunctionSymbol = when (functionSymbol) {
        is IrConstructorSymbol -> functionSymbol.owner.makeConstructorAccessor().symbol
        is IrSimpleFunctionSymbol -> functionSymbol.owner.makeSimpleFunctionAccessor().symbol
        else -> error("Unknown subclass of IrFunctionSymbol")
    }

    private fun IrConstructor.makeConstructorAccessor(): IrConstructor {
        val source = this
        val accessorDescriptor = WrappedClassConstructorDescriptor()
        return IrConstructorImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
            IrConstructorSymbolImpl(accessorDescriptor),
            name,
            Visibilities.PUBLIC,
            IrUninitializedType,
            isInline = false,
            isExternal = false,
            isPrimary = false
        ).also { accessor ->
            accessorDescriptor.bind(accessor)
            accessor.parent = source.parent
            pendingTransformations.add { (accessor.parent as IrDeclarationContainer).declarations.add(accessor) }

            accessor.copyTypeParametersFrom(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.copyValueParametersToStatic(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.returnType = source.returnType.remapTypeParameters(source, accessor)

            val markerParameterDescriptor = WrappedValueParameterDescriptor()
            val markerParameter = IrValueParameterImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
                IrValueParameterSymbolImpl(markerParameterDescriptor),
                Name.identifier("marker"),
                index = accessor.valueParameters.size,
                type = context.ir.symbols.defaultConstructorMarker.owner.defaultType,
                varargElementType = null,
                isCrossinline = false,
                isNoinline = false
            ).apply {
                markerParameterDescriptor.bind(this)
                parent = accessor
            }
            accessor.valueParameters.add(markerParameter)

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
            targetSymbol, targetSymbol.descriptor, targetSymbol.owner.typeParameters.size
        ).also {
            copyAllParamsToArgs(it, accessor)
        }

    private fun IrSimpleFunction.makeSimpleFunctionAccessor(): IrSimpleFunction {
        val source = this
        val accessorDescriptor = WrappedSimpleFunctionDescriptor()
        return IrFunctionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
            IrSimpleFunctionSymbolImpl(accessorDescriptor),
            source.descriptor.accessorName(),
            Visibilities.PUBLIC,
            Modality.FINAL,
            IrUninitializedType,
            isInline = false,
            isExternal = false,
            isTailrec = false,
            isSuspend = source.isSuspend
        ).also { accessor ->
            accessorDescriptor.bind(accessor)
            accessor.parent = source.parent
            pendingTransformations.add { (accessor.parent as IrDeclarationContainer).declarations.add(accessor) }

            accessor.copyTypeParametersFrom(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.copyValueParametersToStatic(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.returnType = source.returnType.remapTypeParameters(source, accessor)

            accessor.body = IrExpressionBodyImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                createSimpleFunctionCall(accessor, source.symbol)
            )
        }
    }

    private fun createSimpleFunctionCall(accessor: IrFunction, targetSymbol: IrFunctionSymbol) =
        IrCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            accessor.returnType,
            targetSymbol, targetSymbol.descriptor,
            targetSymbol.owner.typeParameters.size
        ).also {
            copyAllParamsToArgs(it, accessor)
        }

    private fun makeGetterAccessorSymbol(fieldSymbol: IrFieldSymbol): IrSimpleFunctionSymbol {
        val accessorDescriptor = WrappedSimpleFunctionDescriptor()
        return IrFunctionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
            IrSimpleFunctionSymbolImpl(accessorDescriptor),
            fieldSymbol.descriptor.accessorNameForGetter(),
            Visibilities.PUBLIC,
            Modality.FINAL,
            fieldSymbol.owner.type,
            isInline = false,
            isExternal = false,
            isTailrec = false,
            isSuspend = false
        ).also { accessor ->
            accessorDescriptor.bind(accessor)
            accessor.parent = fieldSymbol.owner.parent
            pendingTransformations.add { (accessor.parent as IrDeclarationContainer).declarations.add(accessor) }

            if (!fieldSymbol.owner.isStatic) {
                val receiverDescriptor = WrappedValueParameterDescriptor()
                accessor.valueParameters.add(
                    IrValueParameterImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
                        IrValueParameterSymbolImpl(receiverDescriptor),
                        Name.identifier("\$this"),
                        index = 0,
                        type = (fieldSymbol.owner.parent as IrClass).defaultType,
                        varargElementType = null,
                        isCrossinline = false,
                        isNoinline = false
                    ).apply {
                        receiverDescriptor.bind(this)
                        parent = accessor
                    }
                )
            }

            accessor.body = createAccessorBodyForGetter(fieldSymbol.owner, accessor)
        }.symbol
    }

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

    private fun makeSetterAccessorSymbol(fieldSymbol: IrFieldSymbol): IrSimpleFunctionSymbol {
        val accessorDescriptor = WrappedSimpleFunctionDescriptor()
        return IrFunctionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
            IrSimpleFunctionSymbolImpl(accessorDescriptor),
            fieldSymbol.descriptor.accessorNameForSetter(),
            Visibilities.PUBLIC,
            Modality.FINAL,
            returnType = context.irBuiltIns.unitType,
            isInline = false,
            isExternal = false,
            isTailrec = false,
            isSuspend = false
        ).also { accessor ->
            accessorDescriptor.bind(accessor)
            accessor.parent = fieldSymbol.owner.parent
            pendingTransformations.add { (accessor.parent as IrDeclarationContainer).declarations.add(accessor) }

            if (!fieldSymbol.owner.isStatic) {
                val receiverDescriptor = WrappedValueParameterDescriptor()
                accessor.valueParameters.add(
                    IrValueParameterImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
                        IrValueParameterSymbolImpl(receiverDescriptor),
                        Name.identifier("\$this"),
                        index = 0,
                        type = (fieldSymbol.owner.parent as IrClass).defaultType,
                        varargElementType = null,
                        isCrossinline = false,
                        isNoinline = false
                    ).apply {
                        receiverDescriptor.bind(this)
                        parent = accessor
                    }
                )
            }
            val valueDescriptor = WrappedValueParameterDescriptor()
            accessor.valueParameters.add(
                IrValueParameterImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
                    IrValueParameterSymbolImpl(valueDescriptor),
                    Name.identifier("value"),
                    index = if (fieldSymbol.owner.isStatic) 0 else 1,
                    type = fieldSymbol.owner.type,
                    varargElementType = null,
                    isCrossinline = false,
                    isNoinline = false
                ).apply {
                    valueDescriptor.bind(this)
                    parent = accessor
                }
            )

            accessor.body = createAccessorBodyForSetter(fieldSymbol.owner, accessor)
        }.symbol
    }

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
                resolvedTargetField.type
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
                accessorSymbol, accessorSymbol.descriptor,
                oldExpression.typeArgumentsCount,
                oldExpression.origin,
                oldExpression.superQualifierSymbol
            )
            is IrDelegatingConstructorCall -> IrDelegatingConstructorCallImpl(
                oldExpression.startOffset, oldExpression.endOffset,
                context.irBuiltIns.unitType,
                accessorSymbol as IrConstructorSymbol, accessorSymbol.descriptor,
                oldExpression.typeArgumentsCount
            )
            else -> error("Need IrCall or IrDelegatingConstructor call, got $oldExpression")
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
                    context.ir.symbols.defaultConstructorMarker.owner.defaultType
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
            accessorSymbol, accessorSymbol.descriptor,
            0,
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
            accessorSymbol, accessorSymbol.descriptor,
            0,
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
        var offset = 0
        val delegateTo = call.symbol.owner
        syntheticFunction.typeParameters.forEachIndexed { i, typeParam ->
            call.putTypeArgument(i, IrSimpleTypeImpl(typeParam.symbol, false, emptyList(), emptyList()))
        }
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

    // !!!!!! Should I use syntheticAccesssorUtils here ???
    private fun FunctionDescriptor.accessorName(): Name {
        val jvmName = DescriptorUtils.getJvmName(this) ?: context.state.typeMapper.mapFunctionName(
            this,
            OwnerKind.getMemberOwnerKind(containingDeclaration)
        )
        return Name.identifier("access\$$jvmName")
    }

    private fun PropertyDescriptor.accessorNameForGetter(): Name {
        val getterName = JvmAbi.getterName(name.asString())
        return Name.identifier("access\$prop\$$getterName")
    }

    private fun PropertyDescriptor.accessorNameForSetter(): Name {
        val setterName = JvmAbi.setterName(name.asString())
        return Name.identifier("access\$prop\$$setterName")
    }

    private fun IrSymbol.isAccessible(): Boolean {
        /// We assume that IR code that reaches us has been checked for correctness at the frontend.
        /// This function needs to single out those cases where Java accessibility rules differ from Kotlin's.

        val declarationRaw = owner as IrDeclarationWithVisibility
        val declaration =
            (declarationRaw as? IrSimpleFunction)?.resolveFakeOverride()
                ?: (declarationRaw as? IrField)?.resolveFakeOverride() ?: declarationRaw

        // There is never a problem with visibility of inline functions, as those don't end up as Java entities
        if (declaration is IrFunction && declaration.isInline) return true

        // The only two visibilities where Kotlin rules differ from JVM rules.
        if (!Visibilities.isPrivate(declaration.visibility) && declaration.visibility != Visibilities.PROTECTED) return true

        val symbolDeclarationContainer = (declaration.parent as? IrDeclarationContainer) as? IrElement
        // If local variables are accessible by Kotlin rules, they also are by Java rules.
        if (symbolDeclarationContainer == null) return true

        // Within inline functions, we have to assume the worst.
        if (inlinedLambdasCollector.isInlineNonpublicContext(allScopes))
            return false

        val contextDeclarationContainer = allScopes.lastOrNull { it.irElement is IrDeclarationContainer }?.irElement

        val samePackage = declaration.getPackageFragment()?.fqName == contextDeclarationContainer?.getPackageFragment()?.fqName
        return when {
            Visibilities.isPrivate(declaration.visibility) && symbolDeclarationContainer != contextDeclarationContainer -> false
            (declaration.visibility == Visibilities.PROTECTED && !samePackage &&
                    !(symbolDeclarationContainer is IrClass && contextDeclarationContainer is IrClass &&
                            contextDeclarationContainer.isSubclassOf(symbolDeclarationContainer))) -> false
            else -> true
        }
    }
}

private class InlinedLambdasCollector : IrElementVisitorVoidWithContext() {
    private val inlinedLambdasInNonPublicContexts = mutableSetOf<IrFunction>()

    fun isInlineNonpublicContext(scopeStack: List<ScopeWithIr>): Boolean {
        val currentFunction = scopeStack.map { it.irElement }. lastOrNull { it is IrFunction } as? IrFunction ?: return false
        return (currentFunction.isInline == true && currentFunction.visibility in setOf(Visibilities.PRIVATE, Visibilities.PROTECTED))
                || currentFunction in inlinedLambdasInNonPublicContexts
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
        val callTarget = expression.symbol.owner
        if (callTarget.isInline && isInlineNonpublicContext(allScopes)) {
            for (i in 0 until expression.valueArgumentsCount) {
                val argument = expression.getValueArgument(i)?.removeBlocks()
                if (argument is IrFunctionReference &&
                    argument.symbol.owner.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA &&
                    !callTarget.valueParameters[i].isNoinline) {
                    inlinedLambdasInNonPublicContexts.add(argument.symbol.owner)
                }
            }
        }
        super.visitFunctionAccess(expression)
    }
}

