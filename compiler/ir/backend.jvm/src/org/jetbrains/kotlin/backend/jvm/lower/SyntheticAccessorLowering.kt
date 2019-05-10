/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.copyValueParametersToStatic
import org.jetbrains.kotlin.backend.common.ir.passTypeArgumentsFrom
import org.jetbrains.kotlin.backend.common.ir.remapTypeParameters
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.intrinsics.receiverAndArgs
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name

internal val syntheticAccessorPhase = makeIrFilePhase(
    ::SyntheticAccessorLowering,
    name = "SyntheticAccessor",
    description = "Introduce synthetic accessors",
    prerequisite = setOf(objectClassPhase)
)

private class SyntheticAccessorLowering(val context: JvmBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {
    private val pendingTransformations = mutableListOf<Function0<Unit>>()

    override fun lower(irFile: IrFile) {
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
        fun makeFunctionAccessorSymbolWithSuper(functionSymbol: IrFunctionSymbol): IrFunctionSymbol =
            makeFunctionAccessorSymbol(functionSymbol, (expression as? IrCall)?.superQualifierSymbol)
        return super.visitExpression(
            handleAccess(
                expression,
                expression.symbol,
                functionMap,
                ::makeFunctionAccessorSymbolWithSuper,
                ::modifyFunctionAccessExpression,
                (expression as? IrCall)?.superQualifierSymbol,
                (expression as? IrCall)?.dispatchReceiver?.type?.classifierOrNull as? IrClassSymbol
            )
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
        exprConverter: (ExprT, ToSyT) -> IrDeclarationReference,
        superQualifierSymbol: IrClassSymbol? = null,
        thisSymbol: IrClassSymbol? = null
    ): IrExpression =
        if (!symbol.isAccessible(superQualifierSymbol != null, thisSymbol)) {
            val accessorSymbol = accumMap.getOrPut(symbol) { symbolConverter(symbol) }
            exprConverter(expression, accessorSymbol)
        } else {
            expression
        }

    private fun makeFunctionAccessorSymbol(functionSymbol: IrFunctionSymbol, superQualifierSymbol: IrClassSymbol?): IrFunctionSymbol =
        when (functionSymbol) {
            is IrConstructorSymbol -> functionSymbol.owner.makeConstructorAccessor().symbol
            is IrSimpleFunctionSymbol -> functionSymbol.owner.makeSimpleFunctionAccessor(superQualifierSymbol).symbol
            else -> error("Unknown subclass of IrFunctionSymbol")
        }

    // In case of Java `protected static`, access could be done from a public inline function in the same package,
    // requiring an accessor, which we cannot add to the Java class.
    private fun IrDeclarationWithVisibility.accessorParent() =
        if (visibility == JavaVisibilities.PROTECTED_STATIC_VISIBILITY)
            currentClass!!.irElement as IrClass
        else
            parent

    private fun IrConstructor.makeConstructorAccessor(): IrConstructor {
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
            targetSymbol, targetSymbol.descriptor,
            targetSymbol.owner.parentAsClass.typeParameters.size + targetSymbol.owner.typeParameters.size
        ).also {
            copyAllParamsToArgs(it, accessor)
        }

    private fun IrSimpleFunction.makeSimpleFunctionAccessor(superQualifierSymbol: IrClassSymbol?): IrSimpleFunction {
        val source = this
        return buildFun {
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = source.accessorName()
            visibility = Visibilities.PUBLIC
            isSuspend = source.isSuspend
        }.also { accessor ->
            accessor.parent = if ((source.parent as? IrClass)?.isInterface == true) {
                // Accessors for interfaces are only for private methods. They should always be placed in DefaultImpls. The only exception
                // is private methods annotated with @JvmDefault; Only in such cases JVM default method is generated.
                // TODO: Handle the exception after targeting Java 8 or newer, where JVM default methods are available.
                context.declarationFactory.getDefaultImplsClass(source.parent as IrClass)
            } else {
                source.accessorParent()
            }
            pendingTransformations.add { (accessor.parent as IrDeclarationContainer).declarations.add(accessor) }

            accessor.copyTypeParametersFrom(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)
            // When a method is defined in class C1 but called on C1's subclass C2, source.dispatchReceiverParameter.type can be resolved
            // to C1, while the method symbol still bound to C2, which expects a receiver of type C2. Therefore, we need to specify
            // dispatchReceiver's type using super qualifier's type explicitly.
            accessor.copyValueParametersToStatic(
                source,
                JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
                superQualifierSymbol?.owner?.defaultType ?: source.dispatchReceiverParameter?.type
            )
            accessor.returnType = source.returnType.remapTypeParameters(source, accessor)

            accessor.body = IrExpressionBodyImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                createSimpleFunctionCall(accessor, source.symbol, superQualifierSymbol)
            )
        }
    }

    private fun createSimpleFunctionCall(accessor: IrFunction, targetSymbol: IrFunctionSymbol, superQualifierSymbol: IrClassSymbol?) =
        IrCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            accessor.returnType,
            targetSymbol, targetSymbol.descriptor,
            targetSymbol.owner.typeParameters.size,
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
                accessorSymbol, accessorSymbol.descriptor,
                oldExpression.typeArgumentsCount,
                oldExpression.origin
            )
            is IrDelegatingConstructorCall -> IrDelegatingConstructorCallImpl(
                oldExpression.startOffset, oldExpression.endOffset,
                context.irBuiltIns.unitType,
                accessorSymbol as IrConstructorSymbol, accessorSymbol.descriptor,
                oldExpression.typeArgumentsCount
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

    private fun IrFunction.accessorName(): Name {
        val jvmName = context.state.typeMapper.mapFunctionName(
            descriptor,
            OwnerKind.getMemberOwnerKind(parentAsClass.descriptor)
        )
        return Name.identifier("access\$$jvmName")
    }

    private fun IrField.accessorNameForGetter(): Name {
        val getterName = JvmAbi.getterName(name.asString())
        return Name.identifier("access\$prop\$$getterName")
    }

    private fun IrField.accessorNameForSetter(): Name {
        val setterName = JvmAbi.setterName(name.asString())
        return Name.identifier("access\$prop\$$setterName")
    }

    private val Visibility.isPrivate
        get() = Visibilities.isPrivate(this)

    private val Visibility.isProtected
        get() = this == Visibilities.PROTECTED ||
                this == JavaVisibilities.PROTECTED_AND_PACKAGE ||
                this == JavaVisibilities.PROTECTED_STATIC_VISIBILITY

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

        // Within inline functions, we have to assume the worst.
        val function = currentFunction?.irElement as IrFunction?
        if (function?.isInline == true && !function.visibility.isPrivate && (withSuper || !function.visibility.isProtected))
            return false

        val contextDeclarationContainer = allScopes.lastOrNull { it.irElement is IrDeclarationContainer }?.irElement

        val samePackage = declaration.getPackageFragment()?.fqName == contextDeclarationContainer?.getPackageFragment()?.fqName
        return when {
            declaration.visibility.isPrivate && symbolDeclarationContainer != contextDeclarationContainer -> false
            declaration.visibility.isProtected && !samePackage &&
                    !(symbolDeclarationContainer is IrClass && contextDeclarationContainer is IrClass &&
                            contextDeclarationContainer.isSubclassOf(symbolDeclarationContainer)) -> false
            // Invoking with super qualifier is implemented by invokespecial, which requires
            // 1. `this` to be assign compatible with current class.
            // 2. the method is a member of a superclass of current class.
            (withSuper && contextDeclarationContainer is IrClass && symbolDeclarationContainer is IrClass &&
                    ((thisObjReference != null && !contextDeclarationContainer.symbol.isSubtypeOfClass(thisObjReference)) ||
                            !(contextDeclarationContainer.isSubclassOf(symbolDeclarationContainer)))) -> false
            else -> true
        }
    }
}
