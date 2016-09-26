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

import org.jetbrains.kotlin.backend.jvm.ClassLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmDeclarationOrigins
import org.jetbrains.kotlin.backend.jvm.codegen.getMemberOwnerKind
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrThisReferenceImpl
import org.jetbrains.kotlin.ir.util.DeepCopyIrTree
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import java.util.*


class InitializersLowering : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val classInitializersBuilder = ClassInitializersBuilder(irClass)
        irClass.acceptChildrenVoid(classInitializersBuilder)

        classInitializersBuilder.transformInstanceInitializerCallsInConstructors(irClass)

        classInitializersBuilder.createStaticInitializationMethod(irClass)
    }

    private class ClassInitializersBuilder(val irClass: IrClass) : IrElementVisitorVoid {
        val classMemberOwnerKind = irClass.descriptor.getMemberOwnerKind()

        val staticInitializerStatements = ArrayList<IrStatement>()

        val instanceInitializerStatements = ArrayList<IrStatement>()

        override fun visitElement(element: IrElement) {
            // skip everything else
        }

        override fun visitField(declaration: IrField) {
            val irFieldInitializer = declaration.initializer?.let { it.expression } ?: return

            val receiver =
                    if (declaration.descriptor.dispatchReceiverParameter != null) // TODO isStaticField
                        IrThisReferenceImpl(irFieldInitializer.startOffset, irFieldInitializer.endOffset,
                                            irClass.descriptor.defaultType, irClass.descriptor)
                    else null
            val irSetField = IrSetFieldImpl(
                    irFieldInitializer.startOffset, irFieldInitializer.endOffset,
                    declaration.descriptor,
                    receiver,
                    irFieldInitializer,
                    null, null
            )

            if (declaration.descriptor.isStatic()) {
                staticInitializerStatements.add(irSetField)
            }
            else {
                instanceInitializerStatements.add(irSetField)
            }
        }

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
            instanceInitializerStatements.addAll(declaration.body.statements)
        }

        private fun CallableMemberDescriptor.isStatic() =
                AsmUtil.isStaticMethod(classMemberOwnerKind, this) // TODO what about properties?


        fun transformInstanceInitializerCallsInConstructors(irClass: IrClass) {
            for (irDeclaration in irClass.declarations) {
                if (irDeclaration !is IrConstructor) continue
                val irBody = irDeclaration.body as IrBlockBody
                if (irBody.statements.any { it is IrInstanceInitializerCall }) {
                    val newStatements = irBody.statements.map { irStatement ->
                        if (irStatement is IrInstanceInitializerCall) {
                            IrBlockImpl(irClass.startOffset, irClass.endOffset, irDeclaration.descriptor.builtIns.unitType, null,
                                        instanceInitializerStatements.map { it.copy() })
                        }
                        else {
                            irStatement
                        }
                    }
                    irBody.statements.clear()
                    irBody.statements.addAll(newStatements)
                }
            }
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
                    IrFunctionImpl(irClass.startOffset, irClass.endOffset, JvmDeclarationOrigins.CLASS_STATIC_INITIALIZER,
                                   staticInitializerDescriptor,
                                   IrBlockBodyImpl(irClass.startOffset, irClass.endOffset,
                                                   staticInitializerStatements.map { it.copy() }))
            )
        }
    }

    companion object {
        val clinitName = Name.special("<clinit>")

        val deepCopyVisitor = DeepCopyIrTree()

        fun IrStatement.copy() = transform(deepCopyVisitor, null)
        fun IrExpression.copy() = transform(deepCopyVisitor, null)
    }
}