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

package org.jetbrains.kotlin.backend.js.declarations

import org.jetbrains.kotlin.backend.js.context.IrTranslationContext
import org.jetbrains.kotlin.backend.js.expression.IrExpressionTranslationVisitor
import org.jetbrains.kotlin.backend.js.util.buildJs
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtensionProperty

class IrDeclarationTranslationVisitor(private val context: IrTranslationContext) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
    }

    override fun visitFunction(declaration: IrFunction) {
        if (declaration.body == null) return
        translateFunction(declaration)
        context.exporter.export(declaration.descriptor)
    }

    override fun visitProperty(declaration: IrProperty) {
        declaration.backingField?.let { backingField ->
            val fieldName = context.naming.backingFieldNames[backingField.descriptor.original]
            context.addDeclaration { fieldName.newVar() }
            backingField.initializer?.let { initializer ->
                val innerVisitor = IrExpressionTranslationVisitor(context)
                context.withStatements(context.fragment.initializerBlock.statements) {
                    val jsInitializer = initializer.expression.accept(innerVisitor, Unit)!!
                    context.addStatement(buildJs { statement(fieldName.ref().assign(jsInitializer)) })
                }
            }
        }

        for (accessor in listOfNotNull(declaration.getter) + listOfNotNull(declaration.setter)) {
            translateFunction(accessor)
        }

        if (declaration.descriptor.isExtensionProperty) {
            for (accessor in declaration.descriptor.accessors) {
                context.exporter.export(accessor)
            }
        }
        else {
            context.exporter.export(declaration.descriptor)
        }
    }

    override fun visitClass(declaration: IrClass) {
        context.translateClass(declaration)
    }

    private fun translateFunction(declaration: IrFunction) {
        val jsFunction = context.translateFunction(declaration)
        jsFunction.name = context.naming.innerNames[declaration.descriptor]
        context.addDeclaration(JsExpressionStatement(jsFunction))
    }
}