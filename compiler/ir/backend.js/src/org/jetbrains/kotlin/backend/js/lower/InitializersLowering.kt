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

package org.jetbrains.kotlin.backend.js.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.*
import java.util.*

class InitializersLowering(val module: IrModuleFragment) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val classInitializersBuilder = ClassInitializersBuilder(irClass)
        irClass.acceptChildrenVoid(classInitializersBuilder)

        classInitializersBuilder.transformInstanceInitializerCallsInConstructors(irClass)
    }

    private inner class ClassInitializersBuilder(val irClass: IrClass) : IrElementVisitorVoid {
        val instanceInitializerStatements = ArrayList<IrStatement>()

        override fun visitElement(element: IrElement) {
            // skip everything else
        }

        override fun visitProperty(declaration: IrProperty) {
            declaration.backingField?.acceptVoid(this)
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

            instanceInitializerStatements.add(irSetField)
        }

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
            instanceInitializerStatements.addAll(declaration.body.statements)
        }

        fun transformInstanceInitializerCallsInConstructors(irClass: IrClass) {
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrExpression {
                    return IrBlockImpl(irClass.startOffset, irClass.endOffset, module.irBuiltins.unit, null,
                                       instanceInitializerStatements.map { it.copy() })
                }
            })
        }
    }

    companion object {
        private fun IrStatement.copy() = deepCopyWithSymbols()
    }
}