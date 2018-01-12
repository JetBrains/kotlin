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
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.OverrideRenderingPolicy
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addIfNotNull

fun IrElement.render() = accept(RenderIrElementVisitor(), null)

class RenderIrElementVisitor : IrElementVisitor<String, Nothing?> {
    override fun visitElement(element: IrElement, data: Nothing?): String =
        "? ${element::class.java.simpleName}"

    override fun visitDeclaration(declaration: IrDeclaration, data: Nothing?): String =
        "? ${declaration::class.java.simpleName} ${declaration.descriptor.ref()}"

    override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): String =
        "MODULE_FRAGMENT name:${declaration.name}"

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: Nothing?): String =
        "EXTERNAL_PACKAGE_FRAGMENT fqName:${declaration.fqName}"

    override fun visitFile(declaration: IrFile, data: Nothing?): String =
        "FILE fqName:${declaration.fqName} fileName:${declaration.name}"

    override fun visitFunction(declaration: IrFunction, data: Nothing?): String =
        "FUN ${declaration.renderOriginIfNonTrivial()}${declaration.renderDeclared()}"

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?): String =
        declaration.run {
            "FUN ${renderOriginIfNonTrivial()}" +
                    "name:$name visibility:$visibility modality:$modality " +
                    renderTypeParameters() + " " +
                    renderValueParameterTypes() + " " +
                    "returnType:$returnType " +
                    "flags:${renderSimpleFunctionFlags()}"
        }

    private fun IrSimpleFunction.renderSimpleFunctionFlags(): String =
        listOfNotNull(
            "tailrec".takeIf { isTailrec },
            "inline".takeIf { isInline },
            "suspend".takeIf { isSuspend }
        ).joinToString(separator = ",")

    private fun IrFunction.renderTypeParameters(): String =
        typeParameters.joinToString(separator = ", ", prefix = "<", postfix = ">") { it.name.toString() }

    private fun IrFunction.renderValueParameterTypes(): String =
        ArrayList<String>().apply {
            addIfNotNull(dispatchReceiverParameter?.run { "\$this:${type.render()}" })
            addIfNotNull(extensionReceiverParameter?.run { "\$receiver:${type.render()}" })
            valueParameters.mapTo(this) { "${it.name}:${it.type.render()}" }
        }.joinToString(separator = ", ", prefix = "(", postfix = ")")

    override fun visitConstructor(declaration: IrConstructor, data: Nothing?): String =
        declaration.run {
            "CONSTRUCTOR ${renderOriginIfNonTrivial()}" +
                    "visibility:$visibility " +
                    renderTypeParameters() + " " +
                    renderValueParameterTypes() + " " +
                    "returnType:${returnType.render()} " +
                    "flags:${if (isInline) "inline" else ""}"
        }

    override fun visitProperty(declaration: IrProperty, data: Nothing?): String =
        declaration.run {
            "PROPERTY ${renderOriginIfNonTrivial()}" +
                    "name:$name type:${type.render()} visibility:$visibility modality:$modality " +
                    "flags:${renderPropertyFlags()}"
        }

    private fun IrProperty.renderPropertyFlags() =
        listOfNotNull(
            "const".takeIf { isConst },
            "lateinit".takeIf { isLateinit },
            "delegated".takeIf { isDelegated },
            if (isVar) "var" else "val"
        ).joinToString(separator = "m")

    override fun visitField(declaration: IrField, data: Nothing?): String =
        "FIELD ${declaration.renderOriginIfNonTrivial()}" +
                "name:${declaration.name} type:${declaration.type.render()} visibility:${declaration.visibility}"

    override fun visitClass(declaration: IrClass, data: Nothing?): String =
        declaration.run {
            "CLASS ${renderOriginIfNonTrivial()}" +
                    "$kind name:$name modality:$modality visibility:$visibility " +
                    "flags:${renderClassFlags()}"
        }

    private fun IrClass.renderClassFlags() =
        listOfNotNull("companion".takeIf { isCompanion }, "data".takeIf { isData }).joinToString(separator = ",")

    override fun visitTypeAlias(declaration: IrTypeAlias, data: Nothing?): String =
        "TYPEALIAS ${declaration.renderOriginIfNonTrivial()}${declaration.descriptor.ref()} type=${declaration.descriptor.underlyingType.render()}"

    override fun visitVariable(declaration: IrVariable, data: Nothing?): String =
        "VAR ${declaration.renderOriginIfNonTrivial()}" +
                "name:${declaration.name} type:${declaration.type.render()} flags:${declaration.renderVariableFlags()}"

    private fun IrVariable.renderVariableFlags(): String =
        listOfNotNull(
            "const".takeIf { isConst },
            "lateinit".takeIf { isLateinit },
            if (isVar) "var" else "val"
        ).joinToString(separator = " ")

    override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?): String =
        "ENUM_ENTRY ${declaration.renderOriginIfNonTrivial()}name:${declaration.name}"

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?): String =
        "ANONYMOUS_INITIALIZER ${declaration.descriptor.ref()}"

    override fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?): String =
        declaration.run {
            "TYPE_PARAMETER ${renderOriginIfNonTrivial()}" +
                    "name:$name index:$index variance:$variance " +
                    "upperBounds:[${upperBounds.joinToString(separator = "; ") { it.render() }}]"
        }

    override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?): String =
        declaration.run {
            "VALUE_PARAMETER ${renderOriginIfNonTrivial()}" +
                    "name:$name " +
                    (if (index >= 0) "index:$index " else "") +
                    "type:${type.render()} " +
                    (varargElementType?.let { "varargElementType:${it.render()} " } ?: "") +
                    "flags:${renderValueParameterFlags()}"
        }

    private fun IrValueParameter.renderValueParameterFlags(): String =
        listOfNotNull(
            "vararg".takeIf { varargElementType != null },
            "crossinline".takeIf { isCrossinline },
            "noinline".takeIf { isNoinline }
        ).joinToString(separator = ",")

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?): String =
        declaration.run {
            "LOCAL_DELEGATED_PROPERTY ${declaration.renderOriginIfNonTrivial()}" +
                    "name:$name type:${type.render()} flags:${renderLocalDelegatedPropertyFlags()}"
        }

    private fun IrLocalDelegatedProperty.renderLocalDelegatedPropertyFlags() =
        if (isVar) "var" else "val"

    override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?): String =
        "EXPRESSION_BODY"

    override fun visitBlockBody(body: IrBlockBody, data: Nothing?): String =
        "BLOCK_BODY"

    override fun visitSyntheticBody(body: IrSyntheticBody, data: Nothing?): String =
        "SYNTHETIC_BODY kind=${body.kind}"

    override fun visitExpression(expression: IrExpression, data: Nothing?): String =
        "? ${expression::class.java.simpleName} type=${expression.type.render()}"

    override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): String =
        "CONST ${expression.kind} type=${expression.type.render()} value=${expression.value}"

    override fun visitVararg(expression: IrVararg, data: Nothing?): String =
        "VARARG type=${expression.type} varargElementType=${expression.varargElementType}"

    override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): String =
        "SPREAD_ELEMENT"

    override fun visitBlock(expression: IrBlock, data: Nothing?): String =
        "BLOCK type=${expression.type.render()} origin=${expression.origin}"

    override fun visitComposite(expression: IrComposite, data: Nothing?): String =
        "COMPOSITE type=${expression.type.render()} origin=${expression.origin}"

    override fun visitReturn(expression: IrReturn, data: Nothing?): String =
        "RETURN type=${expression.type.render()} from=${expression.returnTarget.ref()}'"

    override fun visitCall(expression: IrCall, data: Nothing?): String =
        "CALL '${expression.descriptor.ref()}' ${expression.renderSuperQualifier()}" +
                "type=${expression.type.render()} origin=${expression.origin}"

    private fun IrCall.renderSuperQualifier(): String =
        superQualifier?.let { "superQualifier=${it.name} " } ?: ""

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?): String =
        "DELEGATING_CONSTRUCTOR_CALL '${expression.descriptor.ref()}'"

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?): String =
        "ENUM_CONSTRUCTOR_CALL '${expression.descriptor.ref()}'"

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: Nothing?): String =
        "INSTANCE_INITIALIZER_CALL classDescriptor='${expression.classDescriptor.ref()}'"

    override fun visitGetValue(expression: IrGetValue, data: Nothing?): String =
        "GET_VAR '${expression.descriptor.ref()}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitSetVariable(expression: IrSetVariable, data: Nothing?): String =
        "SET_VAR '${expression.descriptor.ref()}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitGetField(expression: IrGetField, data: Nothing?): String =
        "GET_FIELD '${expression.descriptor.ref()}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitSetField(expression: IrSetField, data: Nothing?): String =
        "SET_FIELD '${expression.descriptor.ref()}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?): String =
        "GET_OBJECT '${expression.descriptor.ref()}' type=${expression.type.render()}"

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?): String =
        "GET_ENUM '${expression.descriptor.ref()}' type=${expression.type.render()}"

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): String =
        "STRING_CONCATENATION type=${expression.type.render()}"

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?): String =
        "TYPE_OP type=${expression.type.render()} origin=${expression.operator} typeOperand=${expression.typeOperand.render()}"

    override fun visitWhen(expression: IrWhen, data: Nothing?): String =
        "WHEN type=${expression.type.render()} origin=${expression.origin}"

    override fun visitBranch(branch: IrBranch, data: Nothing?): String =
        "BRANCH"

    override fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?): String =
        "WHILE label=${loop.label} origin=${loop.origin}"

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Nothing?): String =
        "DO_WHILE label=${loop.label} origin=${loop.origin}"

    override fun visitBreak(jump: IrBreak, data: Nothing?): String =
        "BREAK label=${jump.label} loop.label=${jump.loop.label}"

    override fun visitContinue(jump: IrContinue, data: Nothing?): String =
        "CONTINUE label=${jump.label} loop.label=${jump.loop.label}"

    override fun visitThrow(expression: IrThrow, data: Nothing?): String =
        "THROW type=${expression.type.render()}"

    override fun visitFunctionReference(expression: IrFunctionReference, data: Nothing?): String =
        "FUNCTION_REFERENCE '${expression.descriptor.ref()}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitPropertyReference(expression: IrPropertyReference, data: Nothing?): String =
        buildString {
            append("PROPERTY_REFERENCE ")
            append("'${expression.descriptor.ref()}' ")
            appendNullableAttribute("field=", expression.field) { "'${it.descriptor.ref()}'" }
            appendNullableAttribute("getter=", expression.getter) { "'${it.descriptor.ref()}'" }
            appendNullableAttribute("setter=", expression.setter) { "'${it.descriptor.ref()}'" }
            append("type=${expression.type.render()} ")
            append("origin=${expression.origin}")
        }

    private inline fun <T : Any> StringBuilder.appendNullableAttribute(prefix: String, value: T?, toString: (T) -> String) {
        append(prefix)
        if (value != null) {
            append(toString(value))
        } else {
            append("null")
        }
        append(" ")
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference, data: Nothing?): String =
        buildString {
            append("LOCAL_DELEGATED_PROPERTY_REFERENCE ")
            append("'${expression.descriptor.ref()}' ")
            append("delegate='${expression.delegate.descriptor.ref()}' ")
            append("getter='${expression.getter.descriptor.ref()}' ")
            appendNullableAttribute("setter=", expression.setter) { "'${it.descriptor.ref()}'" }
            append("type=${expression.type.render()} ")
            append("origin=${expression.origin}")
        }

    override fun visitClassReference(expression: IrClassReference, data: Nothing?): String =
        "CLASS_REFERENCE '${expression.descriptor.ref()}' type=${expression.type.render()}"

    override fun visitGetClass(expression: IrGetClass, data: Nothing?): String =
        "GET_CLASS type=${expression.type.render()}"

    override fun visitTry(aTry: IrTry, data: Nothing?): String =
        "TRY type=${aTry.type.render()}"

    override fun visitCatch(aCatch: IrCatch, data: Nothing?): String =
        "CATCH parameter=${aCatch.parameter.ref()}"

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: Nothing?): String =
        "ERROR_DECL ${declaration.descriptor::class.java.simpleName} ${declaration.descriptor.ref()}"

    override fun visitErrorExpression(expression: IrErrorExpression, data: Nothing?): String =
        "ERROR_EXPR '${expression.description}' type=${expression.type.render()}"

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: Nothing?): String =
        "ERROR_CALL '${expression.description}' type=${expression.type.render()}"

    companion object {
        val DECLARATION_RENDERER = DescriptorRenderer.withOptions {
            withDefinedIn = false
            overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE
            includePropertyConstant = true
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
            verbose = false
            modifiers = DescriptorRendererModifier.ALL
        }

        val REFERENCE_RENDERER = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES

        internal fun IrDeclaration.name(): String =
            descriptor.name.toString()

        internal fun DescriptorRenderer.renderDescriptor(descriptor: DeclarationDescriptor): String =
            if (descriptor is ReceiverParameterDescriptor)
                "this@${descriptor.containingDeclaration.name}: ${descriptor.type}"
            else
                render(descriptor)

        internal fun IrDeclaration.renderDeclared(): String =
            DECLARATION_RENDERER.renderDescriptor(this.descriptor)

        internal fun DeclarationDescriptor.ref(): String =
            REFERENCE_RENDERER.renderDescriptor(this)

        internal fun KotlinType.render(): String =
            DECLARATION_RENDERER.renderType(this)

        internal fun IrDeclaration.renderOriginIfNonTrivial(): String =
            if (origin != IrDeclarationOrigin.DEFINED) origin.toString() + " " else ""
    }
}