/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.lower.JvmPropertiesLowering.Companion.createSyntheticMethodForPropertyDelegate
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.util.OperatorNameConventions

internal val singletonOrConstantDelegationPhase = makeIrFilePhase(
    ::SingletonOrConstantDelegationLowering,
    name = "SingletonOrConstantDelegation",
    description = "Optimize `val x by ConstOrSingleton`: there is no need to store the value in a field"
)

private class SingletonOrConstantDelegationLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        if (!context.state.generateOptimizedCallableReferenceSuperClasses) return
        irFile.transform(SingletonOrConstantDelegationTransformer(context), null)
    }
}

private class SingletonOrConstantDelegationTransformer(val context: JvmBackendContext) : IrElementTransformerVoid() {
    override fun visitClass(declaration: IrClass): IrClass {
        declaration.transformChildren(this, null)
        declaration.transformDeclarationsFlat {
            (it as? IrProperty)?.transform()
        }
        return declaration
    }

    private fun IrProperty.transform(): List<IrDeclaration>? {
        if (!isDelegated || isFakeOverride || backingField == null) return null
        val delegate = backingField?.initializer?.expression
        if (delegate?.isConst() != true) {
            return null
        }

        val receiverMapper = object : IrElementTransformer<Name> {
            override fun visitCall(expression: IrCall, data: Name): IrExpression {
                if (expression.symbol.owner.name == data) {
                    if ((expression.dispatchReceiver as? IrGetField)?.symbol == backingField?.symbol) {
                        expression.dispatchReceiver = delegate.deepCopyWithVariables()
                    } else if ((expression.extensionReceiver as? IrGetField)?.symbol == backingField?.symbol) {
                        expression.extensionReceiver = delegate.deepCopyWithVariables()
                    }
                }
                return expression
            }
        }

        getter?.body?.transform(receiverMapper, OperatorNameConventions.GET_VALUE)
        setter?.body?.transform(receiverMapper, OperatorNameConventions.SET_VALUE)

        backingField = null

        val initializerBlock = if (delegate !is IrConst<*> && delegate !is IrGetValue)
            context.irFactory.createAnonymousInitializer(
                delegate.startOffset,
                delegate.endOffset,
                IrDeclarationOrigin.DEFINED,
                IrAnonymousInitializerSymbolImpl(parentAsClass.symbol)
            ).apply {
                body = context.irFactory.createBlockBody(delegate.startOffset, delegate.endOffset, listOf(delegate.deepCopyWithVariables()))
            }
        else null

        val delegateMethod = context.createSyntheticMethodForPropertyDelegate(this).apply {
            body = context.createJvmIrBuilder(symbol).run { irExprBody(delegate.deepCopyWithVariables()) }
        }

        return listOfNotNull(this, initializerBlock, delegateMethod)
    }

    private fun IrExpression.isConst(): Boolean =
        when (this) {
            is IrConst<*>, is IrGetSingletonValue -> true
            is IrCall ->
                dispatchReceiver?.isConst() != false
                        && extensionReceiver?.isConst() != false
                        && symbol.owner.run {
                    modality == Modality.FINAL
                            && origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                            && ((body?.statements?.singleOrNull() as? IrReturn)?.value as? IrGetField)?.symbol?.owner?.isFinal == true
                }
            is IrGetValue ->
                symbol.owner.name == SpecialNames.THIS
            else -> false
        }
}
