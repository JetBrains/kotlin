/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

object SYNTHESIZED_INIT_BLOCK : IrStatementOriginImpl("SYNTHESIZED_INIT_BLOCK")

open class InitializersLowering(
    context: CommonBackendContext
) : InitializersLoweringBase(context) {
    override fun lower(irClass: IrClass) {
        val nonStaticDeclarations = getDeclarationsWithNonStaticInitializers(irClass)
        val instanceInitializerStatements = nonStaticDeclarations.mapNotNull { handleDeclaration(irClass, it) }
        transformInstanceInitializerCallsInConstructors(irClass, instanceInitializerStatements)

        val anonymousInitializers = nonStaticDeclarations.filterTo(hashSetOf()) { it is IrAnonymousInitializer }
        irClass.declarations.removeAll(anonymousInitializers)
    }

    private fun getDeclarationsWithNonStaticInitializers(irClass: IrClass): List<IrDeclaration> =
        irClass.declarations.filter {
            (it is IrField && !it.isStatic) || (it is IrAnonymousInitializer && !it.isStatic)
        }

    private fun transformInstanceInitializerCallsInConstructors(irClass: IrClass, instanceInitializerStatements: List<IrStatement>) {
        irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrExpression {
                val copiedBlock =
                    IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType, null, instanceInitializerStatements)
                        .copy(irClass) as IrBlock
                return IrBlockImpl(irClass.startOffset, irClass.endOffset, context.irBuiltIns.unitType, null, copiedBlock.statements)
            }
        })
    }
}

abstract class InitializersLoweringBase(val context: CommonBackendContext) : ClassLoweringPass {
    protected fun handleDeclaration(irClass: IrClass, declaration: IrDeclaration): IrStatement? = when (declaration) {
        is IrField -> handleField(irClass, declaration)
        is IrAnonymousInitializer -> handleAnonymousInitializer(declaration)
        else -> null
    }

    private fun handleField(irClass: IrClass, declaration: IrField): IrStatement? {
        val irFieldInitializer = declaration.initializer?.expression ?: return null

        val receiver =
            if (!declaration.isStatic) // TODO isStaticField
                IrGetValueImpl(
                    irFieldInitializer.startOffset, irFieldInitializer.endOffset,
                    irClass.thisReceiver!!.type, irClass.thisReceiver!!.symbol
                )
            else null
        return IrSetFieldImpl(
            irFieldInitializer.startOffset, irFieldInitializer.endOffset,
            declaration.symbol,
            receiver,
            irFieldInitializer,
            context.irBuiltIns.unitType,
            null, null
        )
    }

    private fun handleAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement = IrBlockImpl(
        declaration.startOffset, declaration.endOffset,
        context.irBuiltIns.unitType,
        SYNTHESIZED_INIT_BLOCK,
        declaration.body.statements
    )

    protected fun IrStatement.copy(containingDeclaration: IrDeclarationParent): IrStatement = deepCopyWithSymbols(containingDeclaration)
}
