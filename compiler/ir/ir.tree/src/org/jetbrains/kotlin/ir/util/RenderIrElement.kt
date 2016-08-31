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
import org.jetbrains.kotlin.types.KotlinType

fun IrElement.render() = accept(RenderIrElementVisitor(), null)

class RenderIrElementVisitor : IrElementVisitor<String, Nothing?> {
    override fun visitElement(element: IrElement, data: Nothing?): String =
            "? ${element.javaClass.simpleName}"

    override fun visitDeclaration(declaration: IrDeclaration, data: Nothing?): String =
            "? ${declaration.javaClass.simpleName} ${declaration.descriptor?.name}"

    override fun visitFile(declaration: IrFile, data: Nothing?): String =
            "FILE ${declaration.name}"

    override fun visitFunction(declaration: IrFunction, data: Nothing?): String =
            "FUN ${declaration.descriptor.render()}"

    override fun visitProperty(declaration: IrProperty, data: Nothing?): String =
            "PROPERTY ${declaration.descriptor.render()}"

    override fun visitPropertyGetter(declaration: IrPropertyGetter, data: Nothing?): String =
            "PROPERTY_GETTER ${declaration.descriptor.render()}"

    override fun visitPropertySetter(declaration: IrPropertySetter, data: Nothing?): String =
            "PROPERTY_SETTER ${declaration.descriptor.render()}"

    override fun visitClass(declaration: IrClass, data: Nothing?): String =
            "CLASS ${declaration.descriptor.kind} ${declaration.descriptor.name}"

    override fun visitTypeAlias(declaration: IrTypeAlias, data: Nothing?): String =
            "TYPEALIAS ${declaration.descriptor.name} type=${declaration.descriptor.underlyingType.render()}"

    override fun visitVariable(declaration: IrVariable, data: Nothing?): String =
            "VAR ${declaration.descriptor.render()}"

    override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?): String =
            "ENUM_ENTRY ${declaration.descriptor.render()}"

    override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?): String =
            "EXPRESSION_BODY"

    override fun visitBlockBody(body: IrBlockBody, data: Nothing?): String =
            "BLOCK_BODY"

    override fun visitExpression(expression: IrExpression, data: Nothing?): String =
            "? ${expression.javaClass.simpleName} type=${expression.type.render()}"

    override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): String =
            "CONST ${expression.kind} type=${expression.type.render()} value='${expression.value}'"

    override fun visitVararg(expression: IrVararg, data: Nothing?): String =
            "VARARG type=${expression.type} varargElementType=${expression.varargElementType}"

    override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): String =
            "SPREAD_ELEMENT"

    override fun visitBlock(expression: IrBlock, data: Nothing?): String =
            "BLOCK type=${expression.type.render()} operator=${expression.operator}"

    override fun visitReturn(expression: IrReturn, data: Nothing?): String =
            "RETURN type=${expression.type.render()} from=${expression.returnTarget.name}"

    override fun visitGetExtensionReceiver(expression: IrGetExtensionReceiver, data: Nothing?): String =
            "\$RECEIVER of: ${expression.descriptor.containingDeclaration.name} type=${expression.type.render()}"

    override fun visitThisReference(expression: IrThisReference, data: Nothing?): String =
            "THIS ${expression.classDescriptor.render()} type=${expression.type.render()}"

    override fun visitCall(expression: IrCall, data: Nothing?): String =
            "CALL .${expression.descriptor.name} ${expression.renderSuperQualifier()}" +
            "type=${expression.type.render()} operator=${expression.operator}"

    private fun IrCall.renderSuperQualifier(): String =
            superQualifier?.let { "superQualifier=${it.name} " } ?: ""

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?): String =
            "DELEGATING_CONSTRUCTOR_CALL ${expression.descriptor.containingDeclaration.name}"

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?): String =
            "ENUM_CONSTRUCTOR_CALL ${expression.descriptor.containingDeclaration.name} " +
            expression.enumEntryDescriptor.let { enumEntryDescriptor ->
                if (enumEntryDescriptor == null) "super"
                else enumEntryDescriptor.name
            }

    override fun visitNestedInitializersCall(expression: IrNestedInitializersCall, data: Nothing?): String =
            "NESTED_INITIALIZERS_CALL classDescriptor=${expression.classDescriptor.name}"

    override fun visitGetVariable(expression: IrGetVariable, data: Nothing?): String =
            "GET_VAR ${expression.descriptor.name} type=${expression.type.render()} operator=${expression.operator}"

    override fun visitSetVariable(expression: IrSetVariable, data: Nothing?): String =
            "SET_VAR ${expression.descriptor.name} type=${expression.type.render()} operator=${expression.operator}"

    override fun visitGetBackingField(expression: IrGetBackingField, data: Nothing?): String =
            "GET_BACKING_FIELD ${expression.descriptor.name} type=${expression.type.render()} operator=${expression.operator}"

    override fun visitSetBackingField(expression: IrSetBackingField, data: Nothing?): String =
            "SET_BACKING_FIELD ${expression.descriptor.name} type=${expression.type.render()} operator=${expression.operator}"

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?): String =
            "GET_OBJECT ${expression.descriptor.name} type=${expression.type.render()}"

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?): String =
            "GET_ENUM_VALUE ${expression.descriptor.name} type=${expression.type.render()}"

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): String =
            "STRING_CONCATENATION type=${expression.type.render()}"

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?): String =
            "TYPE_OP operator=${expression.operator} typeOperand=${expression.typeOperand.render()}"

    override fun visitWhen(expression: IrWhen, data: Nothing?): String =
            "WHEN type=${expression.type.render()} operator=${expression.operator}"

    override fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?): String =
            "WHILE label=${loop.label} operator=${loop.operator}"

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Nothing?): String =
            "DO_WHILE label=${loop.label} operator=${loop.operator}"

    override fun visitBreak(jump: IrBreak, data: Nothing?): String =
            "BREAK label=${jump.label} depth=${jump.getDepth()}"

    override fun visitContinue(jump: IrContinue, data: Nothing?): String =
            "CONTINUE label=${jump.label} depth=${jump.getDepth()}"

    override fun visitThrow(expression: IrThrow, data: Nothing?): String =
            "THROW type=${expression.type.render()}"

    override fun visitCallableReference(expression: IrCallableReference, data: Nothing?): String =
            "CALLABLE_REFERENCE ${expression.descriptor.render()} type=${expression.type.render()}"

    override fun visitTryCatch(tryCatch: IrTryCatch, data: Nothing?): String =
            "TRY_CATCH type=${tryCatch.type.render()}"

    override fun visitDummyDeclaration(declaration: IrDummyDeclaration, data: Nothing?): String =
            "DUMMY ${declaration.descriptor.javaClass.simpleName} ${declaration.descriptor.name}"

    override fun visitDummyExpression(expression: IrDummyExpression, data: Nothing?): String =
            "DUMMY ${expression.description} type=${expression.type.render()}"

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

        internal fun DeclarationDescriptor?.render(): String =
                this?.let { DESCRIPTOR_RENDERER.render(it) } ?: "<none>"

        internal fun KotlinType?.render(): String =
                this?.let { DESCRIPTOR_RENDERER.renderType(it) } ?: "<no-type>"
    }
}