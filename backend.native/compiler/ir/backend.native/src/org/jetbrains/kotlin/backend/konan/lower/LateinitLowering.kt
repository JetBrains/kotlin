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

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.descriptors.resolveFakeOverride
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.types.TypeUtils

internal class LateinitLowering(
        val context: CommonBackendContext,
        private val generateParameterNameInAssertion: Boolean = false
) : FileLoweringPass {

    private val KOTLIN_FQ_NAME                  = FqName("kotlin")
    private val kotlinPackageScope              = context.ir.irModule.descriptor.getPackage(KOTLIN_FQ_NAME).memberScope
    private val isInitializedPropertyDescriptor = kotlinPackageScope
            .getContributedVariables(Name.identifier("isInitialized"), NoLookupLocation.FROM_BACKEND).single {
                it.extensionReceiverParameter.let {
                    it != null && TypeUtils.getClassDescriptor(it.type) == context.reflectionTypes.kProperty0
                } && !it.isExpect
            }
    private val isInitializedGetterDescriptor   = isInitializedPropertyDescriptor.getter!!

    override fun lower(irFile: IrFile) {
        val lateinitPropertyToField = mutableMapOf<PropertyDescriptor, IrField>()
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitProperty(declaration: IrProperty) {
                super.visitProperty(declaration)

                if (declaration.isLateinit && declaration.descriptor.kind.isReal) {
                    lateinitPropertyToField[declaration.descriptor] = declaration.backingField!!
                }
            }
        })

        irFile.transformChildrenVoid(object : IrBuildingTransformer(context) {

            override fun visitVariable(declaration: IrVariable): IrStatement {
                declaration.transformChildrenVoid(this)

                val descriptor = declaration.descriptor
                if (!descriptor.isLateInit) return declaration

                assert(declaration.initializer == null, { "'lateinit' modifier is not allowed for variables with initializer" })
                assert(!KotlinBuiltIns.isPrimitiveType(descriptor.type), { "'lateinit' modifier is not allowed on primitive types" })
                builder.at(declaration).run {
                    declaration.initializer = irNull()
                }
                return declaration
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val symbol = expression.symbol
                val descriptor = symbol.descriptor as? VariableDescriptor
                if (descriptor == null || !descriptor.isLateInit) return expression

                assert(!KotlinBuiltIns.isPrimitiveType(descriptor.type), { "'lateinit' modifier is not allowed on primitive types" })
                builder.at(expression).run {
                    return irBlock(expression) {
                        // TODO: do data flow analysis to check if value is proved to be not-null.
                        +irIfThen(
                                irEqualsNull(irGet(expression.type, symbol)),
                                throwUninitializedPropertyAccessException(symbol)
                        )
                        +irGet(expression.type, symbol)
                    }
                }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                val descriptor = expression.descriptor
                if (descriptor != isInitializedGetterDescriptor) return expression

                val propertyReference = expression.extensionReceiver!! as IrPropertyReference
                assert(propertyReference.extensionReceiver == null, { "'lateinit' modifier is not allowed on extension properties" })
                val propertyDescriptor = propertyReference.descriptor.resolveFakeOverride().original

                val type = propertyDescriptor.type
                assert(!KotlinBuiltIns.isPrimitiveType(type), { "'lateinit' modifier is not allowed on primitive types" })
                builder.at(expression).run {
                    val field = lateinitPropertyToField[propertyDescriptor]!!
                    val fieldValue = irGetField(propertyReference.dispatchReceiver, field)
                    return irNotEquals(fieldValue, irNull())
                }
            }

            override fun visitProperty(declaration: IrProperty): IrStatement {
                declaration.transformChildrenVoid(this)

                if (!declaration.descriptor.isLateInit || !declaration.descriptor.kind.isReal)
                    return declaration

                val backingField = declaration.backingField!!
                transformGetter(backingField, declaration.getter!!)

                assert(backingField.initializer == null, { "'lateinit' modifier is not allowed for properties with initializer" })
                val irBuilder = context.createIrBuilder(backingField.symbol, declaration.startOffset, declaration.endOffset)
                irBuilder.run {
                    backingField.initializer = irExprBody(irNull())
                }

                return declaration
            }

            private fun transformGetter(backingField: IrField, getter: IrFunction) {
                val type = backingField.descriptor.type
                assert(!KotlinBuiltIns.isPrimitiveType(type), { "'lateinit' modifier is not allowed on primitive types" })
                val irBuilder = context.createIrBuilder(getter.symbol, getter.startOffset, getter.endOffset)
                irBuilder.run {
                    getter.body = irBlockBody {
                        val resultVar = irTemporary(
                                irGetField(getter.dispatchReceiverParameter?.let { irGet(it) }, backingField)
                        )
                        +irIfThenElse(
                                context.irBuiltIns.nothingType,
                                irNotEquals(irGet(resultVar), irNull()),
                                irReturn(irGet(resultVar)),
                                throwUninitializedPropertyAccessException(backingField.symbol)
                        )
                    }
                }
            }
        })
    }

    private fun IrBuilderWithScope.throwUninitializedPropertyAccessException(backingFieldSymbol: IrSymbol) =
            irCall(throwErrorFunction, context.irBuiltIns.nothingType).apply {
                if (generateParameterNameInAssertion) {
                    putValueArgument(
                            0,
                            IrConstImpl.string(
                                    startOffset,
                                    endOffset,
                                    context.irBuiltIns.stringType,
                                    backingFieldSymbol.descriptor.name.asString()
                            )
                    )
                }
            }

    private val throwErrorFunction = context.ir.symbols.ThrowUninitializedPropertyAccessException

}