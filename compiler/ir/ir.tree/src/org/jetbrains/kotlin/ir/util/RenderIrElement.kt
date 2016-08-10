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

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.OverrideRenderingPolicy

class RenderIrElementVisitor : IrElementVisitor<String, Nothing?> {
    override fun visitElement(element: IrElement, data: Nothing?): String =
            "? ${element.javaClass.simpleName}"

    override fun visitDeclaration(declaration: IrDeclaration, data: Nothing?): String =
            "? ${declaration.javaClass.simpleName} ${declaration.descriptor?.name}"

    override fun visitFile(declaration: IrFile, data: Nothing?): String =
            "IrFile ${declaration.name}"

    override fun visitFunction(declaration: IrFunction, data: Nothing?): String =
            "IrFunction ${declaration.descriptor.render()}"

    override fun visitProperty(declaration: IrProperty, data: Nothing?): String =
            "IrProperty ${declaration.descriptor.render()} getter=${declaration.getter?.name()} setter=${declaration.setter?.name()}"

    override fun visitPropertyGetter(declaration: IrPropertyGetter, data: Nothing?): String =
            "IrPropertyGetter ${declaration.descriptor.render()} property=${declaration.property?.name()}"

    override fun visitPropertySetter(declaration: IrPropertySetter, data: Nothing?): String =
            "IrPropertySetter ${declaration.descriptor.render()} property=${declaration.property?.name()}"

    override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?): String =
            "IrExpressionBody"

    override fun visitExpression(expression: IrExpression, data: Nothing?): String =
            "? ${expression.javaClass.simpleName} type=${expression.renderType()}"

    override fun <T> visitLiteral(expression: IrLiteralExpression<T>, data: Nothing?): String =
            "LITERAL ${expression.kind} type=${expression.renderType()} value='${expression.value}'"

    override fun visitBlockExpression(expression: IrBlockExpression, data: Nothing?): String =
            "BLOCK type=${expression.renderType()}"

    override fun visitReturnExpression(expression: IrReturnExpression, data: Nothing?): String =
            "RETURN type=${expression.renderType()}"

    override fun visitVariableReference(expression: IrVariableReference, data: Nothing?): String =
            "VAR ${expression.descriptor.name}"

    override fun visitSingletonReference(expression: IrSingletonReference, data: Nothing?): String =
            "SINGLETON ${expression.descriptor.name}"

    override fun visitExtensionReceiverReference(expression: IrExtensionReceiverReference, data: Nothing?): String =
            "\$RECEIVER of: ${expression.descriptor.containingDeclaration.name}"

    override fun visitThisExpression(expression: IrThisExpression, data: Nothing?): String =
            "THIS ${expression.classDescriptor.render()}"

    override fun visitCallExpression(expression: IrCallExpression, data: Nothing?): String =
            "CALL ${expression.callee.name} ${expression.operator ?: ""}"

    override fun visitDummyExpression(expression: IrDummyExpression, data: Nothing?): String =
            "DUMMY ${expression.description}"

    companion object {
        private val DESCRIPTOR_RENDERER = DescriptorRenderer.withOptions {
            withDefinedIn = false
            overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE
            includePropertyConstant = true
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
            verbose = true
            modifiers = DescriptorRendererModifier.ALL
        }

        internal fun IrDeclaration.name(): String =
                descriptor?.let { it.name.toString() } ?: "<none>"

        internal fun DeclarationDescriptor.render(): String =
                DESCRIPTOR_RENDERER.render(this)

        internal fun IrExpression.renderType(): String =
                DESCRIPTOR_RENDERER.renderType(type)
    }
}