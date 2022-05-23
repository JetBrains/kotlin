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
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.shallowCopy
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

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
        if (delegate !is IrGetSingletonValue && delegate !is IrConst<*>) {
            return null
        }

        object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall) = expression.apply {
                if ((dispatchReceiver as? IrGetField)?.symbol == backingField?.symbol) {
                    dispatchReceiver = delegate.shallowCopy()
                } else if ((extensionReceiver as? IrGetField)?.symbol == backingField?.symbol) {
                    extensionReceiver = delegate.shallowCopy()
                }
            }
        }.apply {
            getter?.body?.transform(this, null)
            setter?.body?.transform(this, null)
        }

        backingField = null

        val initializerBlock = (delegate as? IrGetSingletonValue)?.run {
            context.irFactory.createAnonymousInitializer(
                startOffset, endOffset, IrDeclarationOrigin.DEFINED, IrAnonymousInitializerSymbolImpl(parentAsClass.symbol)
            ).apply {
                body = context.irFactory.createBlockBody(startOffset, endOffset, listOf(delegate.shallowCopy()))
            }
        }

        val delegateMethod = context.createSyntheticMethodForPropertyDelegate(this).apply {
            body = context.createJvmIrBuilder(symbol).run { irExprBody(delegate.shallowCopy()) }
        }

        return listOfNotNull(this, initializerBlock, delegateMethod)
    }
}
