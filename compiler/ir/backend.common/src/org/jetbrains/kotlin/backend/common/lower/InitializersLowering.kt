/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class InitializersLowering(context: CommonBackendContext) : InitializersLoweringBase(context), BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, true)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container !is IrConstructor) return

        val irClass = container.constructedClass
        val instanceInitializerStatements = extractInitializers(irClass) {
            (it is IrField && !it.isStatic && (container.isPrimary || !it.primaryConstructorParameter)) ||
                    (it is IrAnonymousInitializer && !it.isStatic)
        }
        val block = IrBlockImpl(irClass.startOffset, irClass.endOffset, context.irBuiltIns.unitType, null, instanceInitializerStatements)
        // Check that the initializers contain no local classes. Deep-copying them is a disaster for code size, and liable to break randomly.
        block.accept(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) =
                element.acceptChildren(this, null)

            override fun visitClass(declaration: IrClass) =
                throw AssertionError("class in initializer should have been moved out by LocalClassPopupLowering: ${declaration.render()}")
        }, null)

        container.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrExpression =
                block.deepCopyWithSymbols(container)
        })
    }
}

private val IrField.primaryConstructorParameter: Boolean
    get() = (initializer?.expression as? IrGetValue)?.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER

abstract class InitializersLoweringBase(open val context: CommonBackendContext) {
    protected fun extractInitializers(irClass: IrClass, filter: (IrDeclaration) -> Boolean) =
        // TODO What about fields that were added by lowerings? e.g. captured outer class or locals?
        irClass.declarations.mapNotNull { if (it is IrProperty) it.backingField else it }.filter(filter).mapNotNull {
            when (it) {
                is IrField -> handleField(irClass, it)
                is IrAnonymousInitializer -> handleAnonymousInitializer(it)
                else -> null
            }
        }

    private fun handleField(irClass: IrClass, declaration: IrField): IrStatement? =
        declaration.initializer?.run {
            val receiver = if (!declaration.isStatic) // TODO isStaticField
                IrGetValueImpl(startOffset, endOffset, irClass.thisReceiver!!.type, irClass.thisReceiver!!.symbol)
            else
                null
            IrSetFieldImpl(
                startOffset,
                endOffset,
                declaration.symbol,
                receiver,
                expression,
                context.irBuiltIns.unitType,
                IrStatementOrigin.INITIALIZE_FIELD
            )
        }

    private fun handleAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement =
        with(declaration) {
            IrBlockImpl(startOffset, endOffset, context.irBuiltIns.unitType, LoweredStatementOrigins.SYNTHESIZED_INIT_BLOCK, body.statements)
        }
}

// Remove anonymous initializers and set field initializers to `null`
class InitializersCleanupLowering(
    val context: CommonBackendContext,
    private val shouldEraseFieldInitializer: (IrField) -> Boolean = { it.correspondingPropertySymbol?.owner?.isConst != true }
) : DeclarationTransformer {
    override val withLocalDeclarations: Boolean get() = true

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrAnonymousInitializer) return emptyList()

        if (declaration is IrField && declaration.parent is IrClass) {
            if (shouldEraseFieldInitializer(declaration)) {
                declaration.initializer = null
            } else {
                declaration.initializer?.let {
                    declaration.initializer =
                        context.irFactory.createExpressionBody(
                            startOffset = it.startOffset,
                            endOffset = it.endOffset,
                            expression = it.expression.deepCopyWithSymbols(),
                        )
                }
            }
        }

        return null
    }
}
