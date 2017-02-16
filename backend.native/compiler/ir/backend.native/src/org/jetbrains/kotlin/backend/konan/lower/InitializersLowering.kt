package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class InitializersLowering(val context: Context) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        InitializersTransformer(irClass).lowerInitializers()
    }

    private inner class InitializersTransformer(val irClass: IrClass) {
        val initializers = mutableListOf<IrStatement>()

        fun lowerInitializers() {
            collectAndRemoveInitializers()
            lowerConstructors()
        }

        object STATEMENT_ORIGIN_ANONYMOUS_INITIALIZER :
                IrStatementOriginImpl("ANONYMOUS_INITIALIZER")

        private fun collectAndRemoveInitializers() {
            // Do with one traversal in order to preserve initializers order.
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitClass(declaration: IrClass): IrStatement {
                    // Skip nested.
                    return declaration
                }

                override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement {
                    initializers.add(IrBlockImpl(declaration.startOffset, declaration.endOffset,
                            context.builtIns.unitType, STATEMENT_ORIGIN_ANONYMOUS_INITIALIZER, declaration.body.statements))
                    return declaration
                }

                override fun visitField(declaration: IrField): IrStatement {
                    val initializer = declaration.initializer ?: return declaration
                    val propertyDescriptor = declaration.descriptor
                    val startOffset = initializer.startOffset
                    val endOffset = initializer.endOffset
                    initializers.add(IrBlockImpl(startOffset, endOffset, context.builtIns.unitType, STATEMENT_ORIGIN_ANONYMOUS_INITIALIZER,
                            listOf(
                                    IrSetFieldImpl(startOffset, endOffset, propertyDescriptor,
                                            IrGetValueImpl(startOffset, endOffset, irClass.descriptor.thisAsReceiverParameter),
                                            initializer.expression, STATEMENT_ORIGIN_ANONYMOUS_INITIALIZER))))
                    declaration.initializer = null
                    return declaration
                }
            })

            irClass.declarations.transformFlat {
                if (it !is IrAnonymousInitializer)
                    null
                else listOf()
            }
        }

        private fun lowerConstructors() {
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitClass(declaration: IrClass): IrStatement {
                    // Skip nested.
                    return declaration
                }

                override fun visitConstructor(declaration: IrConstructor): IrStatement {
                    val blockBody = declaration.body as? IrBlockBody ?: throw AssertionError("Unexpected constructor body: ${declaration.body}")

                    blockBody.statements.transformFlat {
                        when {
                            it is IrInstanceInitializerCall -> initializers
                            /**
                             * IR for kotlin.Any is:
                             * BLOCK_BODY
                             *   DELEGATING_CONSTRUCTOR_CALL 'constructor Any()'
                             *   INSTANCE_INITIALIZER_CALL classDescriptor='Any'
                             *
                             *   to avoid possible recursion we manually reject body generation for Any.
                             */
                            it is IrDelegatingConstructorCall && irClass.descriptor == context.builtIns.any
                                    && it.descriptor == declaration.descriptor -> listOf()
                            else -> null
                        }
                    }

                    return declaration
                }
            })
        }
    }
}