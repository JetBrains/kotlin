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
import org.jetbrains.kotlin.backend.js.util.buildJs
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtensionProperty

class IrClassDeclarationTranslationVisitor(
        private val context: IrTranslationContext,
        private val classDescriptor: ClassDescriptor
) : IrElementVisitorVoid {
    private val classInnerName = context.naming.innerNames[classDescriptor]

    override fun visitElement(element: IrElement) {
    }

    override fun visitConstructor(declaration: IrConstructor) {
        val descriptor = declaration.descriptor
        if (descriptor.isPrimary) return

        val thisName = JsScope.declareTemporaryName("\$this")
        val jsFunction = context.withAliases(listOf(classDescriptor.thisAsReceiverParameter to thisName.makeRef())) {
            context.translateFunction(declaration)
        }
        jsFunction.parameters.add(JsParameter(thisName))

        jsFunction.body.statements.add(0, buildJs {
            val createNewInstance = "Object".dotPure("create").invoke(classInnerName.refPure().dotPrototype())
            statement(thisName.ref().assign(thisName.ref().or(createNewInstance)))
        })

        jsFunction.body.statements += JsReturn(thisName.makeRef())

        jsFunction.name = context.naming.innerNames[descriptor]
        context.addDeclaration(JsExpressionStatement(jsFunction))
        context.exporter.export(descriptor)
    }

    override fun visitFunction(declaration: IrFunction) {
        if (!declaration.descriptor.kind.isReal || declaration.body == null) return

        val descriptor = declaration.descriptor
        val jsFunction = context.translateFunction(declaration)
        val functionName = context.naming.names[descriptor]
        context.addDeclaration { statement(classInnerName.refPure().dotPrototype().dot(functionName).assign(jsFunction)) }
    }

    override fun visitProperty(declaration: IrProperty) {
        if (!declaration.descriptor.kind.isReal) return

        if (declaration.descriptor.isExtensionProperty) {
            declaration.getter?.let { visitFunction(it) }
            declaration.setter?.let { visitFunction(it) }
            return
        }

        val objectLiteral = JsObjectLiteral(true)

        declaration.getter?.let { getter ->
            objectLiteral.propertyInitializers += JsPropertyInitializer(JsNameRef("get"), context.translateFunction(getter))
        }
        declaration.setter?.let { setter ->
            objectLiteral.propertyInitializers += JsPropertyInitializer(JsNameRef("set"), context.translateFunction(setter))
        }

        if (objectLiteral.propertyInitializers.isNotEmpty()) {
            val propertyName = context.naming.names[declaration.descriptor]
            context.addDeclaration { statement(classInnerName.ref().dotPrototype().defineProperty(propertyName.ident, objectLiteral)) }
        }
    }
}