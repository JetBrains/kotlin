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

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addIfNotNull

fun IrElement.render() =
    accept(RenderIrElementVisitor(), null)

class RenderIrElementVisitor : IrElementVisitor<String, Nothing?> {

    fun renderType(type: IrType) = type.render()

    fun renderSymbolReference(symbol: IrSymbol) = symbol.renderReference()

    private fun IrType.render() =
        "${renderTypeAnnotations(annotations)}${renderTypeInner()}"

    private fun IrType.renderTypeInner() =
        when (this) {
            is IrDynamicType -> "dynamic"

            is IrErrorType -> "IrErrorType"

            is IrSimpleType -> buildString {
                append(classifier.renderClassifierFqn())
                if (arguments.isNotEmpty()) {
                    append(
                        arguments.joinToString(prefix = "<", postfix = ">", separator = ", ") {
                            it.renderTypeArgument()
                        }
                    )
                }
                if (hasQuestionMark) {
                    append('?')
                }
            }

            else -> "{${javaClass.simpleName} $this}"
        }

    private fun IrTypeArgument.renderTypeArgument(): String =
        when (this) {
            is IrStarProjection -> "*"

            is IrTypeProjection -> buildString {
                append(variance.label)
                if (variance != Variance.INVARIANT) append(' ')
                append(type.render())
            }

            else -> "IrTypeArgument[$this]"
        }


    private fun renderTypeAnnotations(annotations: List<IrCall>) =
        if (annotations.isEmpty())
            ""
        else
            annotations.joinToString(prefix = "", postfix = " ", separator = " ") { "@[${it.render()}]" }

    private fun IrSymbol.renderReference() =
        if (isBound)
            owner.accept(symbolReferenceRenderer, null)
        else
            "UNBOUND ${javaClass.simpleName}"

    private val symbolReferenceRenderer = BoundSymbolReferenceRenderer()

    private inner class BoundSymbolReferenceRenderer :
        IrElementVisitor<String, Nothing?> {

        override fun visitElement(element: IrElement, data: Nothing?) =
            element.accept(this@RenderIrElementVisitor, null)

        override fun visitVariable(declaration: IrVariable, data: Nothing?) =
            buildString {
                if (declaration.isVar) append("var ") else append("val ")

                append(declaration.name.asString())
                append(": ")
                append(declaration.type.render())
                append(' ')

                append(declaration.renderVariableFlags())

                renderDeclaredIn(declaration)
            }

        override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?) =
            buildString {
                append(declaration.name.asString())
                append(": ")
                append(declaration.type.render())
                append(' ')

                append(declaration.renderValueParameterFlags())

                renderDeclaredIn(declaration)
            }

        override fun visitFunction(declaration: IrFunction, data: Nothing?) =
            buildString {
                append(declaration.visibility)
                append(' ')

                if (declaration is IrSimpleFunction) {
                    append(declaration.modality.toString().toLowerCase())
                    append(' ')
                }

                when (declaration) {
                    is IrSimpleFunction -> append("fun ")
                    is IrConstructor -> append("constructor ")
                    else -> append("{${declaration.javaClass.simpleName}}")
                }

                append(declaration.name.asString())
                append(' ')

                if (declaration.typeParameters.isNotEmpty()) {
                    appendListWith(declaration.typeParameters, "<", ">", ", ") { typeParameter ->
                        append(typeParameter.name.asString())
                    }
                    append(' ')
                }

                appendListWith(declaration.valueParameters, "(", ")", ", ") { valueParameter ->
                    val varargElementType = valueParameter.varargElementType
                    if (varargElementType != null) {
                        append("vararg ")
                        append(valueParameter.name.asString())
                        append(": ")
                        append(varargElementType.render())
                    } else {
                        append(valueParameter.name.asString())
                        append(": ")
                        append(valueParameter.type.render())
                    }
                }

                if (declaration is IrSimpleFunction) {
                    append(": ")
                    append(declaration.returnType.render())
                }
                append(' ')

                when (declaration) {
                    is IrSimpleFunction -> append(declaration.renderSimpleFunctionFlags())
                    is IrConstructor -> append(declaration.renderConstructorFlags())
                }

                renderDeclaredIn(declaration)
            }

        private fun StringBuilder.renderDeclaredIn(irDeclaration: IrDeclaration) {
            append("declared in ")
            renderParentOfReferencedDeclaration(irDeclaration)
        }

        private fun StringBuilder.renderParentOfReferencedDeclaration(declaration: IrDeclaration) {
            val parent = try {
                declaration.parent
            } catch (e: Exception) {
                append("<no parent>")
                return
            }
            when (parent) {
                is IrPackageFragment -> {
                    val fqn = parent.fqName.asString()
                    append(if (fqn.isEmpty()) "<root>" else fqn)
                }
                is IrDeclaration -> {
                    renderParentOfReferencedDeclaration(parent)
                    append('.')
                    if (parent is IrDeclarationWithName) {
                        append(parent.name)
                    } else {
                        renderElementNameFallback(parent)
                    }
                }
                else ->
                    renderElementNameFallback(parent)
            }
        }

        private fun StringBuilder.renderElementNameFallback(element: Any) {
            append('{')
            append(element.javaClass.simpleName)
            append('}')
        }
    }

    override fun visitElement(element: IrElement, data: Nothing?): String =
        "?ELEMENT? ${element::class.java.simpleName} $element"

    override fun visitDeclaration(declaration: IrDeclaration, data: Nothing?): String =
        "?DECLARATION? ${declaration::class.java.simpleName} $declaration"

    override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): String =
        "MODULE_FRAGMENT name:${declaration.name}"

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: Nothing?): String =
        "EXTERNAL_PACKAGE_FRAGMENT fqName:${declaration.fqName}"

    override fun visitFile(declaration: IrFile, data: Nothing?): String =
        "FILE fqName:${declaration.fqName} fileName:${declaration.path}"

    override fun visitFunction(declaration: IrFunction, data: Nothing?): String =
        "FUN ${declaration.renderOriginIfNonTrivial()}"

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?): String =
        declaration.run {
            "FUN ${renderOriginIfNonTrivial()}" +
                    "name:$name visibility:$visibility modality:$modality " +
                    renderTypeParameters() + " " +
                    renderValueParameterTypes() + " " +
                    "returnType:${returnType.render()} " +
                    renderSimpleFunctionFlags()
        }

    private fun renderFlagsList(vararg flags: String?) =
        flags.filterNotNull().run {
            if (isNotEmpty())
                joinToString(prefix = "[", postfix = "] ", separator = ",")
            else
                ""
        }

    private fun IrSimpleFunction.renderSimpleFunctionFlags(): String =
        renderFlagsList(
            "tailrec".takeIf { isTailrec },
            "inline".takeIf { isInline },
            "external".takeIf { isExternal },
            "suspend".takeIf { isSuspend }
        )

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
                    renderConstructorFlags()
        }

    private fun IrConstructor.renderConstructorFlags() =
        renderFlagsList(
            "inline".takeIf { isInline },
            "external".takeIf { isExternal },
            "primary".takeIf { isPrimary }
        )

    override fun visitProperty(declaration: IrProperty, data: Nothing?): String =
        declaration.run {
            "PROPERTY ${renderOriginIfNonTrivial()}" +
                    "name:$name visibility:$visibility modality:$modality " +
                    renderPropertyFlags()
        }

    private fun IrProperty.renderPropertyFlags() =
        renderFlagsList(
            "external".takeIf { isExternal },
            "const".takeIf { isConst },
            "lateinit".takeIf { isLateinit },
            "delegated".takeIf { isDelegated },
            if (isVar) "var" else "val"
        )

    override fun visitField(declaration: IrField, data: Nothing?): String =
        "FIELD ${declaration.renderOriginIfNonTrivial()}" +
                "name:${declaration.name} type:${declaration.type.render()} visibility:${declaration.visibility} " +
                declaration.renderFieldFlags()

    private fun IrField.renderFieldFlags() =
        renderFlagsList(
            "final".takeIf { isFinal },
            "external".takeIf { isExternal },
            "static".takeIf { isStatic }
        )

    override fun visitClass(declaration: IrClass, data: Nothing?): String =
        declaration.run {
            "CLASS ${renderOriginIfNonTrivial()}" +
                    "$kind name:$name modality:$modality visibility:$visibility " +
                    renderClassFlags() +
                    "superTypes:[${superTypes.joinToString(separator = "; ") { it.render() }}]"
        }

    private fun IrClass.renderClassFlags() =
        renderFlagsList(
            "companion".takeIf { isCompanion },
            "inner".takeIf { isInner },
            "data".takeIf { isData },
            "external".takeIf { isExternal },
            "inline".takeIf { isInline }
        )

    override fun visitVariable(declaration: IrVariable, data: Nothing?): String =
        "VAR ${declaration.renderOriginIfNonTrivial()}" +
                "name:${declaration.name} type:${declaration.type.render()} ${declaration.renderVariableFlags()}"

    private fun IrVariable.renderVariableFlags(): String =
        renderFlagsList(
            "const".takeIf { isConst },
            "lateinit".takeIf { isLateinit },
            if (isVar) "var" else "val"
        )

    override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?): String =
        "ENUM_ENTRY ${declaration.renderOriginIfNonTrivial()}name:${declaration.name}"

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?): String =
        "ANONYMOUS_INITIALIZER isStatic=${declaration.isStatic}"

    override fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?): String =
        declaration.run {
            "TYPE_PARAMETER ${renderOriginIfNonTrivial()}" +
                    "name:$name index:$index variance:$variance " +
                    "superTypes:[${superTypes.joinToString(separator = "; ") { it.render() }}]"
        }

    override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?): String =
        declaration.run {
            "VALUE_PARAMETER ${renderOriginIfNonTrivial()}" +
                    "name:$name " +
                    (if (index >= 0) "index:$index " else "") +
                    "type:${type.render()} " +
                    (varargElementType?.let { "varargElementType:${it.render()} " } ?: "") +
                    renderValueParameterFlags()
        }

    private fun IrValueParameter.renderValueParameterFlags(): String =
        renderFlagsList(
            "vararg".takeIf { varargElementType != null },
            "crossinline".takeIf { isCrossinline },
            "noinline".takeIf { isNoinline }
        )

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
        "CONST ${expression.kind} type=${expression.type.render()} value=${expression.value?.escapeIfRequired()}"

    private fun Any.escapeIfRequired() =
        when (this) {
            is String -> "\"${StringUtil.escapeStringCharacters(this)}\""
            is Char -> "'${StringUtil.escapeStringCharacters(this.toString())}'"
            else -> this
        }

    override fun visitVararg(expression: IrVararg, data: Nothing?): String =
        "VARARG type=${expression.type.render()} varargElementType=${expression.varargElementType.render()}"

    override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): String =
        "SPREAD_ELEMENT"

    override fun visitBlock(expression: IrBlock, data: Nothing?): String =
        "BLOCK type=${expression.type.render()} origin=${expression.origin}"

    override fun visitComposite(expression: IrComposite, data: Nothing?): String =
        "COMPOSITE type=${expression.type.render()} origin=${expression.origin}"

    override fun visitReturn(expression: IrReturn, data: Nothing?): String =
        "RETURN type=${expression.type.render()} from='${expression.returnTargetSymbol.renderReference()}'"

    override fun visitCall(expression: IrCall, data: Nothing?): String =
        "CALL '${expression.symbol.renderReference()}' ${expression.renderSuperQualifier()}" +
                "type=${expression.type.render()} origin=${expression.origin}"

    private fun IrCall.renderSuperQualifier(): String =
        superQualifierSymbol?.let { "superQualifier='${it.renderReference()}' " } ?: ""

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?): String =
        "DELEGATING_CONSTRUCTOR_CALL '${expression.symbol.renderReference()}'"

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?): String =
        "ENUM_CONSTRUCTOR_CALL '${expression.symbol.renderReference()}'"

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: Nothing?): String =
        "INSTANCE_INITIALIZER_CALL classDescriptor='${expression.classSymbol.renderReference()}'"

    override fun visitGetValue(expression: IrGetValue, data: Nothing?): String =
        "GET_VAR '${expression.symbol.renderReference()}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitSetVariable(expression: IrSetVariable, data: Nothing?): String =
        "SET_VAR '${expression.symbol.renderReference()}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitGetField(expression: IrGetField, data: Nothing?): String =
        "GET_FIELD '${expression.symbol.renderReference()}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitSetField(expression: IrSetField, data: Nothing?): String =
        "SET_FIELD '${expression.symbol.renderReference()}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?): String =
        "GET_OBJECT '${expression.symbol.renderReference()}' type=${expression.type.render()}"

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?): String =
        "GET_ENUM '${expression.symbol.renderReference()}' type=${expression.type.render()}"

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
        "FUNCTION_REFERENCE '${expression.symbol.renderReference()}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitPropertyReference(expression: IrPropertyReference, data: Nothing?): String =
        buildString {
            append("PROPERTY_REFERENCE ")
            append("'${expression.symbol.renderReference()}' ")
            appendNullableAttribute("field=", expression.field) { "'${it.renderReference()}'" }
            appendNullableAttribute("getter=", expression.getter) { "'${it.renderReference()}'" }
            appendNullableAttribute("setter=", expression.setter) { "'${it.renderReference()}'" }
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
            append("delegate='${expression.delegate.renderReference()}' ")
            append("getter='${expression.getter.renderReference()}' ")
            appendNullableAttribute("setter=", expression.setter) { "'${it.renderReference()}'" }
            append("type=${expression.type.render()} ")
            append("origin=${expression.origin}")
        }

    override fun visitClassReference(expression: IrClassReference, data: Nothing?): String =
        "CLASS_REFERENCE '${expression.symbol.renderReference()}' type=${expression.type.render()}"

    override fun visitGetClass(expression: IrGetClass, data: Nothing?): String =
        "GET_CLASS type=${expression.type.render()}"

    override fun visitTry(aTry: IrTry, data: Nothing?): String =
        "TRY type=${aTry.type.render()}"

    override fun visitCatch(aCatch: IrCatch, data: Nothing?): String =
        "CATCH parameter=${aCatch.catchParameter.symbol.renderReference()}"

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: Nothing?): String =
        "DYN_OP operator=${expression.operator} type=${expression.type.render()}"

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: Nothing?): String =
        "DYN_MEMBER memberName='${expression.memberName}' type=${expression.type.render()}"

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: Nothing?): String =
        "ERROR_DECL ${declaration.descriptor::class.java.simpleName} ${declaration.descriptor.ref()}"

    override fun visitErrorExpression(expression: IrErrorExpression, data: Nothing?): String =
        "ERROR_EXPR '${expression.description}' type=${expression.type.render()}"

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: Nothing?): String =
        "ERROR_CALL '${expression.description}' type=${expression.type.render()}"
}

@Deprecated("Rewrite descriptor-based code")
private val REFERENCE_RENDERER = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES

internal fun IrDeclaration.name(): String =
    descriptor.name.toString()

internal fun DescriptorRenderer.renderDescriptor(descriptor: DeclarationDescriptor): String =
    if (descriptor is ReceiverParameterDescriptor)
        "this@${descriptor.containingDeclaration.name}: ${descriptor.type}"
    else
        render(descriptor)

internal fun DeclarationDescriptor.ref(): String =
    REFERENCE_RENDERER.renderDescriptor(this.original)

internal fun IrDeclaration.renderOriginIfNonTrivial(): String =
    if (origin != IrDeclarationOrigin.DEFINED) "$origin " else ""

internal fun IrClassifierSymbol.renderClassifierFqn(): String =
    if (isBound)
        when (val owner = owner) {
            is IrClass -> owner.renderClassFqn()
            is IrTypeParameter -> owner.renderTypeParameterFqn()
            else -> "`unexpected classifier: ${owner.render()}`"
        }
    else "<unbound ${this.javaClass.simpleName}>"

internal fun IrClass.renderClassFqn(): String =
    StringBuilder().also { renderDeclarationFqn(it) }.toString()

internal fun IrTypeParameter.renderTypeParameterFqn(): String =
    StringBuilder().also { sb ->
        sb.append(name.asString())
        sb.append(" of ")
        renderDeclarationParentFqn(sb)
    }.toString()

private fun IrDeclaration.renderDeclarationFqn(sb: StringBuilder) {
    renderDeclarationParentFqn(sb)
    sb.append('.')
    if (this is IrDeclarationWithName) {
        sb.append(name.asString())
    } else {
        sb.append(this)
    }
}

private fun IrDeclaration.renderDeclarationParentFqn(sb: StringBuilder) {
    try {
        val parent = this.parent
        if (parent is IrDeclaration) {
            parent.renderDeclarationFqn(sb)
        } else if (parent is IrPackageFragment) {
            sb.append(parent.fqName.toString())
        }
    } catch (e: UninitializedPropertyAccessException) {
        sb.append("<uninitialized parent>")
    }
}

fun IrType.render() = RenderIrElementVisitor().renderType(this)

internal inline fun <T> StringBuilder.appendListWith(
    list: List<T>,
    prefix: String,
    postfix: String,
    separator: String,
    renderItem: StringBuilder.(T) -> Unit
) {
    append(prefix)
    var isFirst = true
    for (item in list) {
        if (!isFirst) append(separator)
        renderItem(item)
        isFirst = false
    }
    append(postfix)
}
