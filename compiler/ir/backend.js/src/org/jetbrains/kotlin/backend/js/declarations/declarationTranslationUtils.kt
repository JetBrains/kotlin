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
import org.jetbrains.kotlin.backend.js.context.getSuggestedName
import org.jetbrains.kotlin.backend.js.util.JsBuilder
import org.jetbrains.kotlin.backend.js.util.buildJs
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.backend.ast.*

fun IrTranslationContext.translateFunction(declaration: IrFunction): JsFunction {
    val jsFunction = JsFunction(scope, JsBlock(), "")
    nestedDeclaration(declaration.descriptor) {
        val aliases = mutableListOf<Pair<DeclarationDescriptor, JsExpression>>()

        declaration.extensionReceiverParameter?.let { extensionReceiver ->
            val receiverName = JsScope.declareTemporaryName("\$receiver")
            jsFunction.parameters += JsParameter(receiverName)
            aliases += extensionReceiver.descriptor to receiverName.makeRef()
        }

        for (parameter in declaration.valueParameters) {
            jsFunction.parameters += JsParameter(naming.innerNames[parameter.descriptor])
        }

        withAliases(aliases) {
            declaration.body?.let { body ->
                withStatements(jsFunction.body.statements) {
                    body.acceptVoid(IrBodyTranslationVisitor(this))
                }
            }
        }
    }
    return jsFunction
}

fun IrTranslationContext.addDeclaration(statement: JsStatement) {
    fragment.declarationBlock.statements += statement
}

fun IrTranslationContext.addDeclaration(builder: JsBuilder.() -> JsStatement) = addDeclaration(buildJs(builder))

fun IrTranslationContext.translateClass(declaration: IrClass) {
    if (declaration.descriptor.isExternal) return

    nestedDeclaration(declaration.descriptor) {
        withAliases(listOf(declaration.descriptor.thisAsReceiverParameter to JsThisRef())) {
            val constructor = declaration.declarations.filterIsInstance<IrConstructor>().singleOrNull { it.descriptor.isPrimary }
            val jsFunction = if (constructor != null) {
                translateFunction(constructor)
            }
            else {
                JsFunction(scope, JsBlock(), "")
            }
            jsFunction.name = naming.innerNames[declaration.descriptor]
            addDeclaration(JsExpressionStatement(jsFunction))

            val classVisitor = IrClassDeclarationTranslationVisitor(this, declaration.descriptor)
            for (innerDeclaration in declaration.declarations) {
                innerDeclaration.acceptVoid(classVisitor)
            }

            exporter.export(declaration.descriptor)

            if (declaration.descriptor.kind == ClassKind.OBJECT) {
                addObjectInstanceFunction(declaration, jsFunction.body.statements)
            }
        }
    }

    val model = ClassModelGenerator(naming, module.descriptor).generateClassModel(declaration.descriptor)
    fragment.classes[model.name] = model
}

private fun IrTranslationContext.addObjectInstanceFunction(declaration: IrClass, constructorBody: MutableList<JsStatement>) {
    val instanceFieldName = JsScope.declareTemporaryName(getSuggestedName(declaration.descriptor) + "_instance")
    addDeclaration { instanceFieldName.newVar(JsNullLiteral()) }

    val function = JsFunction(scope, JsBlock(), "")
    addDeclaration(JsExpressionStatement(function))

    val constructorName = naming.innerNames[declaration.descriptor]
    with (function.body) {
        statements += buildJs {
            JsIf(
                    instanceFieldName.refPure().strictEq(JsNullLiteral()),
                    statement(constructorName.refPure().newInstance())
            )
        }
        statements += JsReturn(instanceFieldName.makeRef())
    }

    function.name = naming.objectInnerNames[declaration.descriptor]

    constructorBody.add(0, buildJs { statement(instanceFieldName.ref().assign(JsThisRef())) })
}