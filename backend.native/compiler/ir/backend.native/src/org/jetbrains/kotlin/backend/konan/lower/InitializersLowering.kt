/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.createDispatchReceiverParameter
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.descriptorUtils.hasPrimaryConstructor

internal class InitializersLowering(val context: CommonBackendContext) : ClassLoweringPass {

    object STATEMENT_ORIGIN_ANONYMOUS_INITIALIZER : IrStatementOriginImpl("ANONYMOUS_INITIALIZER")

    object DECLARATION_ORIGIN_ANONYMOUS_INITIALIZER : IrDeclarationOriginImpl("ANONYMOUS_INITIALIZER")

    override fun lower(irClass: IrClass) {
        InitializersTransformer(irClass).lowerInitializers()
    }

    private inner class InitializersTransformer(val irClass: IrClass) {
        val initializers = mutableListOf<IrStatement>()

        fun lowerInitializers() {
            collectAndRemoveInitializers()
            val initializerMethodSymbol = createInitializerMethod()
            lowerConstructors(initializerMethodSymbol)
        }

        private fun collectAndRemoveInitializers() {
            // Do with one traversal in order to preserve initializers order.
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitClass(declaration: IrClass): IrStatement {
                    // Skip nested.
                    return declaration
                }

                override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement {
                    initializers.add(IrBlockImpl(declaration.startOffset, declaration.endOffset,
                            context.irBuiltIns.unitType, STATEMENT_ORIGIN_ANONYMOUS_INITIALIZER, declaration.body.statements))
                    return declaration
                }

                override fun visitField(declaration: IrField): IrStatement {
                    val initializer = declaration.initializer ?: return declaration
                    val startOffset = initializer.startOffset
                    val endOffset = initializer.endOffset
                    initializers.add(IrBlockImpl(startOffset, endOffset, context.irBuiltIns.unitType, STATEMENT_ORIGIN_ANONYMOUS_INITIALIZER,
                            listOf(
                                    IrSetFieldImpl(startOffset, endOffset, declaration.symbol,
                                            IrGetValueImpl(
                                                    startOffset, endOffset,
                                                    irClass.thisReceiver!!.type, irClass.thisReceiver!!.symbol
                                            ),
                                            initializer.expression,
                                            context.irBuiltIns.unitType,
                                            STATEMENT_ORIGIN_ANONYMOUS_INITIALIZER))))
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

        private fun createInitializerMethod(): IrSimpleFunctionSymbol? {
            if (irClass.descriptor.hasPrimaryConstructor())
                return null // Place initializers in the primary constructor.
            val initializerMethodDescriptor = SimpleFunctionDescriptorImpl.create(
                    /* containingDeclaration        = */ irClass.descriptor,
                    /* annotations                  = */ Annotations.EMPTY,
                    /* name                         = */ "INITIALIZER".synthesizedName,
                    /* kind                         = */ CallableMemberDescriptor.Kind.DECLARATION,
                    /* source                       = */ SourceElement.NO_SOURCE)
            initializerMethodDescriptor.initialize(
                    /* receiverParameterType        = */ null,
                    /* dispatchReceiverParameter    = */ irClass.descriptor.thisAsReceiverParameter,
                    /* typeParameters               = */ listOf(),
                    /* unsubstitutedValueParameters = */ listOf(),
                    /* returnType                   = */ context.builtIns.unitType,
                    /* modality                     = */ Modality.FINAL,
                    /* visibility                   = */ Visibilities.PRIVATE)
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset
            val initializer = IrFunctionImpl(startOffset, endOffset, DECLARATION_ORIGIN_ANONYMOUS_INITIALIZER,
                    initializerMethodDescriptor)

            initializer.returnType = context.irBuiltIns.unitType
            initializer.body = IrBlockBodyImpl(startOffset, endOffset, initializers)

            initializer.parent = irClass
            initializer.createDispatchReceiverParameter()

            initializers.forEach {
                it.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        if (expression.symbol == irClass.thisReceiver!!.symbol) {
                            return IrGetValueImpl(
                                    expression.startOffset,
                                    expression.endOffset,
                                    initializer.dispatchReceiverParameter!!.type,
                                    initializer.dispatchReceiverParameter!!.symbol
                            )
                        } else {
                            return expression
                        }
                    }
                })
            }

            irClass.declarations.add(initializer)

            return initializer.symbol
        }

        private fun lowerConstructors(initializerMethodSymbol: IrSimpleFunctionSymbol?) {
            if (irClass.kind == ClassKind.ANNOTATION_CLASS)
                return
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {

                override fun visitClass(declaration: IrClass): IrStatement {
                    // Skip nested.
                    return declaration
                }

                override fun visitConstructor(declaration: IrConstructor): IrStatement {
                    val blockBody = declaration.body as? IrBlockBody
                            ?: throw AssertionError("Unexpected constructor body: ${declaration.body}")

                    blockBody.statements.transformFlat {
                        when {
                            it is IrInstanceInitializerCall -> {
                                if (initializerMethodSymbol == null) {
                                    assert(declaration.descriptor.isPrimary)
                                    initializers
                                } else {
                                    val startOffset = it.startOffset
                                    val endOffset = it.endOffset
                                    listOf(IrCallImpl(startOffset, endOffset,
                                            context.irBuiltIns.unitType, initializerMethodSymbol
                                    ).apply {
                                        dispatchReceiver = IrGetValueImpl(
                                                startOffset, endOffset,
                                                irClass.thisReceiver!!.type, irClass.thisReceiver!!.symbol
                                        )
                                    })
                                }
                            }
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