/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import java.util.*


class InitializersLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val classInitializersBuilder = ClassInitializersBuilder(irClass)
        irClass.acceptChildrenVoid(classInitializersBuilder)

        classInitializersBuilder.transformInstanceInitializerCallsInConstructors(irClass)

        classInitializersBuilder.createStaticInitializationMethod(irClass)
    }

    private inner class ClassInitializersBuilder(val irClass: IrClass) : IrElementVisitorVoid {
        val staticInitializerStatements = ArrayList<IrStatement>()

        val instanceInitializerStatements = ArrayList<IrStatement>()

        override fun visitElement(element: IrElement) {
            // skip everything else
        }

        override fun visitField(declaration: IrField) {
            val irFieldInitializer = declaration.initializer?.expression ?: return

            val receiver =
                    if (declaration.descriptor.dispatchReceiverParameter != null) // TODO isStaticField
                        IrGetValueImpl(irFieldInitializer.startOffset, irFieldInitializer.endOffset,
                                       irClass.descriptor.thisAsReceiverParameter)
                    else null
            val irSetField = IrSetFieldImpl(
                    irFieldInitializer.startOffset, irFieldInitializer.endOffset,
                    declaration.descriptor,
                    receiver,
                    irFieldInitializer,
                    null, null
            )

            if (DescriptorUtils.isStaticDeclaration(declaration.descriptor)) {
                staticInitializerStatements.add(irSetField)
            }
            else {
                instanceInitializerStatements.add(irSetField)
            }
        }

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
            instanceInitializerStatements.addAll(declaration.body.statements)
        }

        fun transformInstanceInitializerCallsInConstructors(irClass: IrClass) {
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrExpression {
                    return IrBlockImpl(irClass.startOffset, irClass.endOffset, context.builtIns.unitType, null,
                                       instanceInitializerStatements.map { it.copy() })
                }
            })
        }

        fun createStaticInitializationMethod(irClass: IrClass) {
            val staticInitializerDescriptor = SimpleFunctionDescriptorImpl.create(
                    irClass.descriptor, Annotations.EMPTY, clinitName,
                    CallableMemberDescriptor.Kind.SYNTHESIZED,
                    SourceElement.NO_SOURCE
            )
            staticInitializerDescriptor.initialize(
                    null, null, emptyList(), emptyList(),
                    irClass.descriptor.builtIns.unitType,
                    Modality.FINAL, Visibilities.PUBLIC
            )
            irClass.declarations.add(
                    IrFunctionImpl(irClass.startOffset, irClass.endOffset, JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER,
                                   staticInitializerDescriptor,
                                   IrBlockBodyImpl(irClass.startOffset, irClass.endOffset,
                                                   staticInitializerStatements.map { it.copy() }))
            )
        }
    }

    companion object {
        val clinitName = Name.special("<clinit>")

        fun IrStatement.copy() = deepCopyWithSymbols()
        fun IrExpression.copy() = deepCopyWithSymbols()
    }
}