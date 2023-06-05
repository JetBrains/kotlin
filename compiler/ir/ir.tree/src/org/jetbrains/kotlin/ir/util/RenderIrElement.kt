/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.ReturnTypeIsNotInitializedException
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

fun IrElement.render(options: DumpIrTreeOptions = DumpIrTreeOptions()) =
    accept(RenderIrElementVisitor(options), null)

class RenderIrElementVisitor(private val options: DumpIrTreeOptions = DumpIrTreeOptions()) :
    IrElementVisitor<String, Nothing?> {

    private val variableNameData = VariableNameData(options.normalizeNames)

    fun renderType(type: IrType) = type.renderTypeWithRenderer(this@RenderIrElementVisitor, options)

    fun renderSymbolReference(symbol: IrSymbol) = symbol.renderReference()

    fun renderAsAnnotation(irAnnotation: IrConstructorCall): String =
        StringBuilder().also { it.renderAsAnnotation(irAnnotation, this, options) }.toString()

    private fun IrType.render(): String =
        this.renderTypeWithRenderer(this@RenderIrElementVisitor, options)

    private fun IrSymbol.renderReference() =
        if (isBound)
            owner.accept(BoundSymbolReferenceRenderer(variableNameData, options), null)
        else
            "UNBOUND ${javaClass.simpleName}"

    private class BoundSymbolReferenceRenderer(
        private val variableNameData: VariableNameData,
        private val options: DumpIrTreeOptions,
    ) : IrElementVisitor<String, Nothing?> {

        override fun visitElement(element: IrElement, data: Nothing?) = buildTrimEnd {
            append('{')
            append(element.javaClass.simpleName)
            append('}')
            if (element is IrDeclaration) {
                if (element is IrDeclarationWithName) {
                    append(element.name)
                    append(' ')
                }
                renderDeclaredIn(element)
            }
        }

        override fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?): String =
            renderTypeParameter(declaration, null, options)

        override fun visitClass(declaration: IrClass, data: Nothing?) =
            renderClassWithRenderer(declaration, null, options)

        override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?) =
            renderEnumEntry(declaration)

        override fun visitField(declaration: IrField, data: Nothing?) =
            renderField(declaration, null, options)

        override fun visitVariable(declaration: IrVariable, data: Nothing?) =
            buildTrimEnd {
                if (declaration.isVar) append("var ") else append("val ")

                append(declaration.normalizedName(variableNameData))
                append(": ")
                append(declaration.type.renderTypeWithRenderer(null, options))
                append(' ')

                if (options.printFlagsInDeclarationReferences) {
                    append(declaration.renderVariableFlags())
                }

                renderDeclaredIn(declaration)
            }

        override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?) =
            buildTrimEnd {
                append(declaration.name.asString())
                append(": ")
                append(declaration.type.renderTypeWithRenderer(null, options))
                append(' ')

                if (options.printFlagsInDeclarationReferences) {
                    append(declaration.renderValueParameterFlags())
                }

                renderDeclaredIn(declaration)
            }

        override fun visitFunction(declaration: IrFunction, data: Nothing?) =
            buildTrimEnd {
                append(declaration.visibility)
                append(' ')

                if (declaration is IrSimpleFunction) {
                    append(declaration.modality.toString().toLowerCaseAsciiOnly())
                    append(' ')
                }

                when (declaration) {
                    is IrSimpleFunction -> append("fun ")
                    is IrConstructor -> append("constructor ")
                    else -> append("{${declaration.javaClass.simpleName}}")
                }

                append(declaration.name.asString())
                append(' ')

                renderTypeParameters(declaration)

                appendIterableWith(declaration.valueParameters, "(", ")", ", ") { valueParameter ->
                    val varargElementType = valueParameter.varargElementType
                    if (varargElementType != null) {
                        append("vararg ")
                        append(valueParameter.name.asString())
                        append(": ")
                        append(varargElementType.renderTypeWithRenderer(null, options))
                    } else {
                        append(valueParameter.name.asString())
                        append(": ")
                        append(valueParameter.type.renderTypeWithRenderer(null, options))
                    }
                }

                if (declaration is IrSimpleFunction) {
                    append(": ")
                    append(declaration.renderReturnType(null, options))
                }
                append(' ')

                if (options.printFlagsInDeclarationReferences) {
                    when (declaration) {
                        is IrSimpleFunction -> append(declaration.renderSimpleFunctionFlags())
                        is IrConstructor -> append(declaration.renderConstructorFlags())
                    }
                }

                renderDeclaredIn(declaration)
            }

        private fun StringBuilder.renderTypeParameters(declaration: IrTypeParametersContainer) {
            if (declaration.typeParameters.isNotEmpty()) {
                appendIterableWith(declaration.typeParameters, "<", ">", ", ") { typeParameter ->
                    append(typeParameter.name.asString())
                }
                append(' ')
            }
        }

        override fun visitProperty(declaration: IrProperty, data: Nothing?) =
            buildTrimEnd {
                append(declaration.visibility)
                append(' ')
                append(declaration.modality.toString().toLowerCaseAsciiOnly())
                append(' ')

                append(declaration.name.asString())

                val getter = declaration.getter
                if (getter != null) {
                    append(": ")
                    append(getter.renderReturnType(null, options))
                } else declaration.backingField?.type?.let { type ->
                    append(": ")
                    append(type.renderTypeWithRenderer(null, options))
                }

                if (options.printFlagsInDeclarationReferences) {
                    append(' ')
                    append(declaration.renderPropertyFlags())
                }
            }

        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?): String =
            buildTrimEnd {
                if (declaration.isVar) append("var ") else append("val ")
                append(declaration.name.asString())
                append(": ")
                append(declaration.type.renderTypeWithRenderer(null, options))
                append(" by (...)")
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
                    val fqn = parent.packageFqName.asString()
                    append(fqn.ifEmpty { "<root>" })
                }
                is IrDeclaration -> {
                    renderParentOfReferencedDeclaration(parent)
                    appendDeclarationNameToFqName(parent, options) {
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

    override fun visitDeclaration(declaration: IrDeclarationBase, data: Nothing?): String =
        "?DECLARATION? ${declaration::class.java.simpleName} $declaration"

    override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): String =
        "MODULE_FRAGMENT name:${declaration.name}"

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: Nothing?): String =
        "EXTERNAL_PACKAGE_FRAGMENT fqName:${declaration.packageFqName}"

    override fun visitFile(declaration: IrFile, data: Nothing?): String =
        "FILE fqName:${declaration.packageFqName} fileName:${declaration.path}"

    override fun visitFunction(declaration: IrFunction, data: Nothing?): String =
        declaration.runTrimEnd {
            "FUN ${renderOriginIfNonTrivial()}"
        }

    override fun visitScript(declaration: IrScript, data: Nothing?) = "SCRIPT"

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?): String =
        declaration.runTrimEnd {
            "FUN ${renderOriginIfNonTrivial()}" +
                    "name:$name visibility:$visibility modality:$modality " +
                    renderTypeParameters() + " " +
                    renderValueParameterTypes() + " " +
                    "returnType:${renderReturnType(this@RenderIrElementVisitor, options)} " +
                    renderSimpleFunctionFlags()
        }

    private fun IrFunction.renderValueParameterTypes(): String =
        ArrayList<String>().apply {
            addIfNotNull(dispatchReceiverParameter?.run { "\$this:${type.render()}" })
            addIfNotNull(extensionReceiverParameter?.run { "\$receiver:${type.render()}" })
            valueParameters.mapTo(this) { "${it.name}:${it.type.render()}" }
        }.joinToString(separator = ", ", prefix = "(", postfix = ")")

    override fun visitConstructor(declaration: IrConstructor, data: Nothing?): String =
        declaration.runTrimEnd {
            "CONSTRUCTOR ${renderOriginIfNonTrivial()}" +
                    "visibility:$visibility " +
                    renderTypeParameters() + " " +
                    renderValueParameterTypes() + " " +
                    "returnType:${renderReturnType(this@RenderIrElementVisitor, options)} " +
                    renderConstructorFlags()
        }

    override fun visitProperty(declaration: IrProperty, data: Nothing?): String =
        declaration.runTrimEnd {
            "PROPERTY ${renderOriginIfNonTrivial()}" +
                    "name:$name visibility:$visibility modality:$modality " +
                    renderPropertyFlags()
        }

    override fun visitField(declaration: IrField, data: Nothing?): String =
        renderField(declaration, this, options)

    override fun visitClass(declaration: IrClass, data: Nothing?): String =
        renderClassWithRenderer(declaration, this, options)

    override fun visitVariable(declaration: IrVariable, data: Nothing?): String =
        declaration.runTrimEnd {
            "VAR ${renderOriginIfNonTrivial()}name:${normalizedName(variableNameData)} type:${type.render()} ${renderVariableFlags()}"
        }

    override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?): String =
        renderEnumEntry(declaration)

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?): String =
        "ANONYMOUS_INITIALIZER isStatic=${declaration.isStatic}"

    override fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?): String =
        renderTypeParameter(declaration, this, options)

    override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?): String =
        declaration.runTrimEnd {
            "VALUE_PARAMETER ${renderOriginIfNonTrivial()}" +
                    "name:$name " +
                    (if (index >= 0) "index:$index " else "") +
                    "type:${type.render()} " +
                    (varargElementType?.let { "varargElementType:${it.render()} " } ?: "") +
                    renderValueParameterFlags()
        }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?): String =
        declaration.runTrimEnd {
            "LOCAL_DELEGATED_PROPERTY ${declaration.renderOriginIfNonTrivial()}" +
                    "name:$name type:${type.render()} flags:${renderLocalDelegatedPropertyFlags()}"
        }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: Nothing?): String =
        declaration.run {
            "TYPEALIAS ${declaration.renderOriginIfNonTrivial()}" +
                    "name:$name visibility:$visibility expandedType:${expandedType.render()}" +
                    renderTypeAliasFlags()
        }

    override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?): String =
        "EXPRESSION_BODY"

    override fun visitBlockBody(body: IrBlockBody, data: Nothing?): String =
        "BLOCK_BODY"

    override fun visitSyntheticBody(body: IrSyntheticBody, data: Nothing?): String =
        "SYNTHETIC_BODY kind=${body.kind}"

    override fun visitExpression(expression: IrExpression, data: Nothing?): String =
        "? ${expression::class.java.simpleName} type=${expression.type.render()}"

    override fun visitConst(expression: IrConst<*>, data: Nothing?): String =
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

    override fun visitBlock(expression: IrBlock, data: Nothing?): String {
        val prefix = when (expression) {
            is IrReturnableBlock -> "RETURNABLE_"
            is IrInlinedFunctionBlock -> "INLINED_"
            else -> ""
        }
        return "${prefix}BLOCK type=${expression.type.render()} origin=${expression.origin}"
    }

    override fun visitComposite(expression: IrComposite, data: Nothing?): String =
        "COMPOSITE type=${expression.type.render()} origin=${expression.origin}"

    override fun visitReturn(expression: IrReturn, data: Nothing?): String =
        "RETURN type=${expression.type.render()} from='${expression.returnTargetSymbol.renderReference()}'"

    override fun visitCall(expression: IrCall, data: Nothing?): String =
        "CALL '${expression.symbol.renderReference()}' ${expression.renderSuperQualifier()}" +
                "type=${expression.type.render()} origin=${expression.origin}"

    private fun IrCall.renderSuperQualifier(): String =
        superQualifierSymbol?.let { "superQualifier='${it.renderReference()}' " } ?: ""

    override fun visitConstructorCall(expression: IrConstructorCall, data: Nothing?): String =
        "CONSTRUCTOR_CALL '${expression.symbol.renderReference()}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?): String =
        "DELEGATING_CONSTRUCTOR_CALL '${expression.symbol.renderReference()}'"

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?): String =
        "ENUM_CONSTRUCTOR_CALL '${expression.symbol.renderReference()}'"

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: Nothing?): String =
        "INSTANCE_INITIALIZER_CALL classDescriptor='${expression.classSymbol.renderReference()}'"

    override fun visitGetValue(expression: IrGetValue, data: Nothing?): String =
        "GET_VAR '${expression.symbol.renderReference()}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitSetValue(expression: IrSetValue, data: Nothing?): String =
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
        "FUNCTION_REFERENCE '${expression.symbol.renderReference()}' " +
                "type=${expression.type.render()} origin=${expression.origin} " +
                "reflectionTarget=${renderReflectionTarget(expression)}"

    override fun visitRawFunctionReference(expression: IrRawFunctionReference, data: Nothing?): String =
        "RAW_FUNCTION_REFERENCE '${expression.symbol.renderReference()}' type=${expression.type.render()}"

    private fun renderReflectionTarget(expression: IrFunctionReference) =
        if (expression.symbol == expression.reflectionTarget)
            "<same>"
        else
            expression.reflectionTarget?.renderReference()

    override fun visitPropertyReference(expression: IrPropertyReference, data: Nothing?): String =
        buildTrimEnd {
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
        buildTrimEnd {
            append("LOCAL_DELEGATED_PROPERTY_REFERENCE ")
            append("'${expression.symbol.renderReference()}' ")
            append("delegate='${expression.delegate.renderReference()}' ")
            append("getter='${expression.getter.renderReference()}' ")
            appendNullableAttribute("setter=", expression.setter) { "'${it.renderReference()}'" }
            append("type=${expression.type.render()} ")
            append("origin=${expression.origin}")
        }

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: Nothing?): String =
        buildTrimEnd {
            append("FUN_EXPR type=${expression.type.render()} origin=${expression.origin}")
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

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: Nothing?): String =
        "ERROR_DECL ${declaration.descriptor::class.java.simpleName} " +
                descriptorRendererForErrorDeclarations.renderDescriptor(declaration.descriptor.original)

    override fun visitErrorExpression(expression: IrErrorExpression, data: Nothing?): String =
        "ERROR_EXPR '${expression.description}' type=${expression.type.render()}"

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: Nothing?): String =
        "ERROR_CALL '${expression.description}' type=${expression.type.render()}"

    override fun visitConstantArray(expression: IrConstantArray, data: Nothing?): String =
        "CONSTANT_ARRAY type=${expression.type.render()}"

    override fun visitConstantObject(expression: IrConstantObject, data: Nothing?): String =
        "CONSTANT_OBJECT type=${expression.type.render()} constructor=${expression.constructor.renderReference()}"

    override fun visitConstantPrimitive(expression: IrConstantPrimitive, data: Nothing?): String =
        "CONSTANT_PRIMITIVE type=${expression.type.render()}"


    private val descriptorRendererForErrorDeclarations = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES
}

internal fun DescriptorRenderer.renderDescriptor(descriptor: DeclarationDescriptor): String =
    if (descriptor is ReceiverParameterDescriptor)
        "this@${descriptor.containingDeclaration.name}: ${descriptor.type}"
    else
        render(descriptor)

internal fun IrDeclaration.renderOriginIfNonTrivial(): String =
    if (origin != IrDeclarationOrigin.DEFINED) "$origin " else ""

internal fun IrClassifierSymbol.renderClassifierFqn(options: DumpIrTreeOptions): String =
    if (isBound)
        when (val owner = owner) {
            is IrClass -> owner.renderClassFqn(options)
            is IrScript -> owner.renderScriptFqn(options)
            is IrTypeParameter -> owner.renderTypeParameterFqn(options)
            else -> "`unexpected classifier: ${owner.render(options)}`"
        }
    else
        "<unbound ${this.javaClass.simpleName}>"

internal fun IrTypeAliasSymbol.renderTypeAliasFqn(options: DumpIrTreeOptions): String =
    if (isBound)
        StringBuilder().also { owner.renderDeclarationFqn(it, options) }.toString()
    else
        "<unbound $this>"

internal fun IrClass.renderClassFqn(options: DumpIrTreeOptions): String =
    StringBuilder().also { renderDeclarationFqn(it, options) }.toString()

internal fun IrScript.renderScriptFqn(options: DumpIrTreeOptions): String =
    StringBuilder().also { renderDeclarationFqn(it, options) }.toString()

internal fun IrTypeParameter.renderTypeParameterFqn(options: DumpIrTreeOptions): String =
    StringBuilder().also { sb ->
        sb.append(name.asString())
        sb.append(" of ")
        renderDeclarationParentFqn(sb, options)
    }.toString()

private inline fun StringBuilder.appendDeclarationNameToFqName(
    declaration: IrDeclaration,
    options: DumpIrTreeOptions,
    fallback: () -> Unit
) {
    if (!declaration.isFileClass || options.printFacadeClassInFqNames) {
        append('.')
        if (declaration is IrDeclarationWithName) {
            append(declaration.name)
        } else {
            fallback()
        }
    }
}

private fun IrDeclaration.renderDeclarationFqn(sb: StringBuilder, options: DumpIrTreeOptions) {
    renderDeclarationParentFqn(sb, options)
    sb.appendDeclarationNameToFqName(this, options) {
        sb.append(this)
    }
}

private fun IrDeclaration.renderDeclarationParentFqn(sb: StringBuilder, options: DumpIrTreeOptions) {
    try {
        val parent = this.parent
        if (parent is IrDeclaration) {
            parent.renderDeclarationFqn(sb, options)
        } else if (parent is IrPackageFragment) {
            sb.append(parent.packageFqName.toString())
        }
    } catch (e: UninitializedPropertyAccessException) {
        sb.append("<uninitialized parent>")
    }
}

fun IrType.render(options: DumpIrTreeOptions = DumpIrTreeOptions()) =
    renderTypeWithRenderer(RenderIrElementVisitor(options), options)

fun IrSimpleType.render(options: DumpIrTreeOptions = DumpIrTreeOptions()) = (this as IrType).render(options)

fun IrTypeArgument.render(options: DumpIrTreeOptions = DumpIrTreeOptions()) =
    when (this) {
        is IrStarProjection -> "*"
        is IrTypeProjection -> "$variance ${type.render(options)}"
    }

internal inline fun <T, Buffer : Appendable> Buffer.appendIterableWith(
    iterable: Iterable<T>,
    prefix: String,
    postfix: String,
    separator: String,
    renderItem: Buffer.(T) -> Unit
) {
    append(prefix)
    var isFirst = true
    for (item in iterable) {
        if (!isFirst) append(separator)
        renderItem(item)
        isFirst = false
    }
    append(postfix)
}

private inline fun buildTrimEnd(fn: StringBuilder.() -> Unit): String =
    buildString(fn).trimEnd()

private inline fun <T> T.runTrimEnd(fn: T.() -> String): String =
    run(fn).trimEnd()

private fun renderFlagsList(vararg flags: String?) =
    flags.filterNotNull().run {
        if (isNotEmpty())
            joinToString(prefix = "[", postfix = "] ", separator = ",")
        else
            ""
    }

private fun IrClass.renderClassFlags() =
    renderFlagsList(
        "companion".takeIf { isCompanion },
        "inner".takeIf { isInner },
        "data".takeIf { isData },
        "external".takeIf { isExternal },
        "value".takeIf { isValue },
        "expect".takeIf { isExpect },
        "fun".takeIf { isFun }
    )

private fun IrField.renderFieldFlags() =
    renderFlagsList(
        "final".takeIf { isFinal },
        "external".takeIf { isExternal },
        "static".takeIf { isStatic },
    )

private fun IrSimpleFunction.renderSimpleFunctionFlags(): String =
    renderFlagsList(
        "tailrec".takeIf { isTailrec },
        "inline".takeIf { isInline },
        "external".takeIf { isExternal },
        "suspend".takeIf { isSuspend },
        "expect".takeIf { isExpect },
        "fake_override".takeIf { isFakeOverride },
        "operator".takeIf { isOperator },
        "infix".takeIf { isInfix }
    )

private fun IrConstructor.renderConstructorFlags() =
    renderFlagsList(
        "inline".takeIf { isInline },
        "external".takeIf { isExternal },
        "primary".takeIf { isPrimary },
        "expect".takeIf { isExpect }
    )

private fun IrProperty.renderPropertyFlags() =
    renderFlagsList(
        "external".takeIf { isExternal },
        "const".takeIf { isConst },
        "lateinit".takeIf { isLateinit },
        "delegated".takeIf { isDelegated },
        "expect".takeIf { isExpect },
        "fake_override".takeIf { isFakeOverride },
        if (isVar) "var" else "val"
    )

private fun IrVariable.renderVariableFlags(): String =
    renderFlagsList(
        "const".takeIf { isConst },
        "lateinit".takeIf { isLateinit },
        if (isVar) "var" else "val"
    )

private fun IrValueParameter.renderValueParameterFlags(): String =
    renderFlagsList(
        "vararg".takeIf { varargElementType != null },
        "crossinline".takeIf { isCrossinline },
        "noinline".takeIf { isNoinline },
        "assignable".takeIf { isAssignable }
    )

private fun IrTypeAlias.renderTypeAliasFlags(): String =
    renderFlagsList(
        "actual".takeIf { isActual }
    )

private fun IrFunction.renderTypeParameters(): String =
    typeParameters.joinToString(separator = ", ", prefix = "<", postfix = ">") { it.name.toString() }

private val IrFunction.safeReturnType: IrType?
    get() = try {
        returnType
    } catch (e: ReturnTypeIsNotInitializedException) {
        null
    }

private fun IrLocalDelegatedProperty.renderLocalDelegatedPropertyFlags() =
    if (isVar) "var" else "val"

private class VariableNameData(val normalizeNames: Boolean) {
    val nameMap: MutableMap<IrVariableSymbol, String> = mutableMapOf()
    var temporaryIndex: Int = 0
}

private fun IrVariable.normalizedName(data: VariableNameData): String {
    if (data.normalizeNames && (origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE || origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR)) {
        return data.nameMap.getOrPut(symbol) { "tmp_${data.temporaryIndex++}" }
    }
    return name.asString()
}

private fun IrFunction.renderReturnType(renderer: RenderIrElementVisitor?, options: DumpIrTreeOptions): String =
    safeReturnType?.renderTypeWithRenderer(renderer, options) ?: "<Uninitialized>"

private fun IrType.renderTypeWithRenderer(renderer: RenderIrElementVisitor?, options: DumpIrTreeOptions): String =
    "${renderTypeAnnotations(annotations, renderer, options)}${renderTypeInner(renderer, options)}"

private fun IrType.renderTypeInner(renderer: RenderIrElementVisitor?, options: DumpIrTreeOptions) =
    when (this) {
        is IrDynamicType -> "dynamic"

        is IrErrorType -> "IrErrorType(${options.verboseErrorTypes.ifTrue { originalKotlinType }})"

        is IrSimpleType -> buildTrimEnd {
            val isDefinitelyNotNullType =
                classifier is IrTypeParameterSymbol && nullability == SimpleTypeNullability.DEFINITELY_NOT_NULL
            if (isDefinitelyNotNullType) append("{")
            append(classifier.renderClassifierFqn(options))
            if (arguments.isNotEmpty()) {
                append(
                    arguments.joinToString(prefix = "<", postfix = ">", separator = ", ") {
                        it.renderTypeArgument(renderer, options)
                    }
                )
            }
            if (isDefinitelyNotNullType) {
                append(" & Any}")
            } else if (isMarkedNullable()) {
                append('?')
            }
            abbreviation?.let {
                append(it.renderTypeAbbreviation(renderer, options))
            }
        }

        else -> "{${javaClass.simpleName} $this}"
    }

private fun IrTypeAbbreviation.renderTypeAbbreviation(renderer: RenderIrElementVisitor?, options: DumpIrTreeOptions): String =
    buildString {
        append("{ ")
        append(renderTypeAnnotations(annotations, renderer, options))
        append(typeAlias.renderTypeAliasFqn(options))
        if (arguments.isNotEmpty()) {
            append(
                arguments.joinToString(prefix = "<", postfix = ">", separator = ", ") {
                    it.renderTypeArgument(renderer, options)
                }
            )
        }
        if (hasQuestionMark) {
            append('?')
        }
        append(" }")
    }

private fun IrTypeArgument.renderTypeArgument(renderer: RenderIrElementVisitor?, options: DumpIrTreeOptions): String =
    when (this) {
        is IrStarProjection -> "*"

        is IrTypeProjection -> buildTrimEnd {
            append(variance.label)
            if (variance != Variance.INVARIANT) append(' ')
            append(type.renderTypeWithRenderer(renderer, options))
        }
    }

private fun renderTypeAnnotations(annotations: List<IrConstructorCall>, renderer: RenderIrElementVisitor?, options: DumpIrTreeOptions) =
    if (annotations.isEmpty())
        ""
    else
        buildString {
            appendIterableWith(annotations, prefix = "", postfix = " ", separator = " ") {
                append("@[")
                renderAsAnnotation(it, renderer, options)
                append("]")
            }
        }

private fun StringBuilder.renderAsAnnotation(
    irAnnotation: IrConstructorCall,
    renderer: RenderIrElementVisitor?,
    options: DumpIrTreeOptions,
) {
    val annotationClassName = irAnnotation.symbol.takeIf { it.isBound }?.owner?.parentAsClass?.name?.asString() ?: "<unbound>"
    append(annotationClassName)

    if (irAnnotation.typeArgumentsCount != 0) {
        (0 until irAnnotation.typeArgumentsCount).joinTo(this, ", ", "<", ">") { i ->
            irAnnotation.getTypeArgument(i)?.renderTypeWithRenderer(renderer, options) ?: "null"
        }
    }

    if (irAnnotation.valueArgumentsCount == 0) return

    val valueParameterNames = irAnnotation.getValueParameterNamesForDebug()

    appendIterableWith(0 until irAnnotation.valueArgumentsCount, separator = ", ", prefix = "(", postfix = ")") {
        append(valueParameterNames[it])
        append(" = ")
        renderAsAnnotationArgument(irAnnotation.getValueArgument(it), renderer, options)
    }
}

private fun StringBuilder.renderAsAnnotationArgument(irElement: IrElement?, renderer: RenderIrElementVisitor?, options: DumpIrTreeOptions) {
    when (irElement) {
        null -> append("<null>")
        is IrConstructorCall -> renderAsAnnotation(irElement, renderer, options)
        is IrConst<*> -> {
            append('\'')
            append(irElement.value.toString())
            append('\'')
        }
        is IrVararg -> {
            appendIterableWith(irElement.elements, prefix = "[", postfix = "]", separator = ", ") {
                renderAsAnnotationArgument(it, renderer, options)
            }
        }
        else -> if (renderer != null) {
            append(irElement.accept(renderer, null))
        } else {
            append("...")
        }
    }
}

private fun renderClassWithRenderer(declaration: IrClass, renderer: RenderIrElementVisitor?, options: DumpIrTreeOptions) =
    declaration.runTrimEnd {
        "CLASS ${renderOriginIfNonTrivial()}" +
                "$kind name:$name modality:$modality visibility:$visibility " +
                renderClassFlags() +
                "superTypes:[${superTypes.joinToString(separator = "; ") { it.renderTypeWithRenderer(renderer, options) }}]"
    }

private fun renderEnumEntry(declaration: IrEnumEntry) = declaration.runTrimEnd {
    "ENUM_ENTRY ${renderOriginIfNonTrivial()}name:$name"
}

private fun renderField(declaration: IrField, renderer: RenderIrElementVisitor?, options: DumpIrTreeOptions) = declaration.runTrimEnd {
    "FIELD ${renderOriginIfNonTrivial()}name:$name type:${
        type.renderTypeWithRenderer(
            renderer,
            options
        )
    } visibility:$visibility ${renderFieldFlags()}"
}

private fun renderTypeParameter(declaration: IrTypeParameter, renderer: RenderIrElementVisitor?, options: DumpIrTreeOptions) =
    declaration.runTrimEnd {
        "TYPE_PARAMETER ${renderOriginIfNonTrivial()}" +
                "name:$name index:$index variance:$variance " +
                "superTypes:[${
                    superTypes.joinToString(separator = "; ") {
                        it.renderTypeWithRenderer(
                            renderer, options
                        )
                    }
                }] " +
                "reified:$isReified"
    }
