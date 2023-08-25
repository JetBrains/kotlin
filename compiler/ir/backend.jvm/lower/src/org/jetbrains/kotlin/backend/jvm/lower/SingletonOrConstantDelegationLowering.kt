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
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.util.isFileClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.remapReceiver
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal val singletonOrConstantDelegationPhase = makeIrFilePhase(
    ::SingletonOrConstantDelegationLowering,
    name = "SingletonOrConstantDelegation",
    description = "Optimize `val x by ConstOrSingleton`: there is no need to store the value in a field"
)

private class SingletonOrConstantDelegationLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        if (!context.config.generateOptimizedCallableReferenceSuperClasses) return
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
        val delegate = getSingletonOrConstantForOptimizableDelegatedProperty() ?: return null
        val originalThis = parentAsClass.thisReceiver

        class DelegateFieldAccessTransformer(val newReceiver: IrExpression) : IrElementTransformerVoid() {
            override fun visitGetField(expression: IrGetField): IrExpression =
                if (expression.symbol == backingField?.symbol) newReceiver else super.visitGetField(expression)
        }

        getter?.transform(DelegateFieldAccessTransformer(delegate.remapReceiver(originalThis, getter?.dispatchReceiverParameter)), null)
        setter?.transform(DelegateFieldAccessTransformer(delegate.remapReceiver(originalThis, setter?.dispatchReceiverParameter)), null)

        backingField = null

        val initializerBlock = if (delegate !is IrConst<*> && delegate !is IrGetValue)
            context.irFactory.createAnonymousInitializer(
                delegate.startOffset,
                delegate.endOffset,
                IrDeclarationOrigin.DEFINED,
                IrAnonymousInitializerSymbolImpl(parentAsClass.symbol),
                parentAsClass.isFileClass
            ).apply {
                body = context.irFactory.createBlockBody(delegate.startOffset, delegate.endOffset, listOf(delegate.remapReceiver(null, null)))
            }
        else null

        val delegateMethod = context.createSyntheticMethodForPropertyDelegate(this).apply {
            body = context.createJvmIrBuilder(symbol).run { irExprBody(delegate.remapReceiver(originalThis, dispatchReceiverParameter)) }
        }

        return listOfNotNull(this, initializerBlock, delegateMethod)
    }
}
