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
            "??? ${element.javaClass.simpleName}"

    override fun visitDeclaration(declaration: IrDeclaration, data: Nothing?): String =
            "??? ${declaration.javaClass.simpleName} ${declaration.descriptor.name}"

    override fun visitFile(declaration: IrFile, data: Nothing?): String =
            "IrFile ${declaration.name}"

    override fun visitFunction(declaration: IrFunction, data: Nothing?): String =
            "IrFunction ${declaration.renderDescriptor()}"

    override fun visitProperty(declaration: IrProperty, data: Nothing?): String =
            "IrProperty ${declaration.renderDescriptor()}"

    override fun visitPropertyGetter(declaration: IrPropertyGetter, data: Nothing?): String =
            "IrPropertyGetter ${declaration.renderDescriptor()}"

    override fun visitPropertySetter(declaration: IrPropertySetter, data: Nothing?): String =
            "IrPropertySetter ${declaration.renderDescriptor()}"

    override fun visitExpression(expression: IrExpression, data: Nothing?): String =
            "??? ${expression.javaClass.simpleName} type=${expression.renderType()}"

    override fun visitStringLiteral(expression: IrStringLiteral, data: Nothing?): String =
            "IrStringLiteral ${expression.value}"

    override fun visitIntLiteral(expression: IrIntLiteral, data: Nothing?): String =
            "IrIntLiteral type=${expression.renderType()} value=${expression.value}"

    override fun visitBlockExpression(expression: IrBlockExpression, data: Nothing?): String =
            "IrBlockExpression type=${expression.renderType()}"

    override fun visitReturnExpression(expression: IrReturnExpression, data: Nothing?): String =
            "IrReturnExpression type=${expression.renderType()}"

    companion object {
        private val DESCRIPTOR_RENDERER = DescriptorRenderer.withOptions {
            withDefinedIn = false
            overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE
            includePropertyConstant = true
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
            verbose = true
            modifiers = DescriptorRendererModifier.ALL
        }

        private fun IrDeclaration.renderDescriptor(): String = DESCRIPTOR_RENDERER.render(descriptor)
        private fun IrExpression.renderType(): String = DESCRIPTOR_RENDERER.renderType(type)
    }
}