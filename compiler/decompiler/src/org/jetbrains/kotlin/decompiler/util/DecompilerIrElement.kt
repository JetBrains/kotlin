/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.decompiler.getValueParameterNamesForDebug
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.Variance

fun IrElement.render() =
    accept(DecompilerIrElementVisitor(), null)

class DecompilerIrElementVisitor : IrElementVisitor<String, Nothing?> {

    fun renderType(type: IrType) = type.render()

    fun renderAsAnnotation(irAnnotation: IrConstructorCall): String =
        StringBuilder().also { it.renderAsAnnotation(irAnnotation) }.toString()

    private fun StringBuilder.renderAsAnnotation(irAnnotation: IrConstructorCall) {
        val annotationClassName = try {
            irAnnotation.symbol.owner.parentAsClass.name.asString()
        } catch (e: Exception) {
            "<unbound>"
        }
        append(annotationClassName)

        if (irAnnotation.valueArgumentsCount == 0) return

        val valueParameterNames = irAnnotation.getValueParameterNamesForDebug()
        var first = true
        append("(")
        for (i in 0 until irAnnotation.valueArgumentsCount) {
            if (first) {
                first = false
            } else {
                append(", ")
            }
            append(valueParameterNames[i])
            append(" = ")
            renderAsAnnotationArgument(irAnnotation.getValueArgument(i))
        }
        append(")")
    }

    private fun StringBuilder.renderAsAnnotationArgument(irElement: IrElement?) {
        when (irElement) {
            null -> append("<null>")
            is IrConstructorCall -> renderAsAnnotation(irElement)
            is IrConst<*> -> {
                append('\'')
                append(irElement.value.toString())
                append('\'')
            }
            is IrVararg -> {
                appendListWith(irElement.elements, "[", "]", ", ") {
                    renderAsAnnotationArgument(it)
                }
            }
            else -> append(irElement.accept(this@DecompilerIrElementVisitor, null))
        }
    }

    private inline fun buildTrimEnd(fn: StringBuilder.() -> Unit): String =
        buildString(fn).trimEnd()

    private inline fun <T> T.runTrimEnd(fn: T.() -> String): String =
        run(fn).trimEnd()

    private fun IrType.render() =
        "${renderTypeAnnotations(annotations)}${renderTypeInner()}"

    private fun IrType.renderTypeInner() =
        when (this) {
            is IrDynamicType -> "dynamic"

            is IrErrorType -> "IrErrorType"

            is IrSimpleType -> buildTrimEnd {
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
                abbreviation?.let {
                    append(it.renderTypeAbbreviation())
                }
            }

            else -> "{${javaClass.simpleName} $this}"
        }

    private fun IrTypeAbbreviation.renderTypeAbbreviation(): String =
        buildString {
            append("{ ")
            append(renderTypeAnnotations(annotations))
            append(typeAlias.renderTypeAliasFqn())
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
            append(" }")
        }

    private fun IrTypeArgument.renderTypeArgument(): String =
        when (this) {
            is IrStarProjection -> "*"

            is IrTypeProjection -> buildTrimEnd {
                append(variance.label)
                if (variance != Variance.INVARIANT) append(' ')
                append(type.render())
            }

            else -> "IrTypeArgument[$this]"
        }


    private fun renderTypeAnnotations(annotations: List<IrConstructorCall>) =
        if (annotations.isEmpty())
            ""
        else
            annotations.joinToString(prefix = "", postfix = " ", separator = " ") { "@[${renderAsAnnotation(it)}]" }

    private fun IrSymbol.renderReference() =
        if (isBound)
            owner.accept(symbolReferenceRenderer, null)
        else
            "UNBOUND ${javaClass.simpleName}"

    private val symbolReferenceRenderer = BoundSymbolReferenceRenderer()

    private inner class BoundSymbolReferenceRenderer :
        IrElementVisitor<String, Nothing?> {

        override fun visitElement(element: IrElement, data: Nothing?) =
            element.accept(this@DecompilerIrElementVisitor, null)

        override fun visitVariable(declaration: IrVariable, data: Nothing?) =
            buildTrimEnd {
                if (declaration.isVar) append("var ") else append("val ")

                append(declaration.name.asString())
                append(": ")
                append(declaration.type.render())
                append(' ')

                append(declaration.renderVariableFlags())

                renderDeclaredIn(declaration)
            }

        override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?) =
            buildTrimEnd {
                append(declaration.name.asString())
                append(": ")
                append(declaration.type.render())
                append(' ')

                append(declaration.renderValueParameterFlags())

                renderDeclaredIn(declaration)
            }

        override fun visitFunction(declaration: IrFunction, data: Nothing?) =
            buildTrimEnd {
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

                renderTypeParameters(declaration)

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

        private fun StringBuilder.renderTypeParameters(declaration: IrTypeParametersContainer) {
            if (declaration.typeParameters.isNotEmpty()) {
                appendListWith(declaration.typeParameters, "<", ">", ", ") { typeParameter ->
                    append(typeParameter.name.asString())
                }
                append(' ')
            }
        }

        override fun visitProperty(declaration: IrProperty, data: Nothing?) =
            buildTrimEnd {
                append(declaration.visibility)
                append(' ')
                append(declaration.modality.toString().toLowerCase())
                append(' ')

                append(declaration.name.asString())

                val type = declaration.getter?.returnType ?: declaration.backingField?.type
                if (type != null) {
                    append(": ")
                    append(type.render())
                }

                append(' ')
                append(declaration.renderPropertyFlags())
            }

        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?): String =
            buildTrimEnd {
                if (declaration.isVar) append("var ") else append("val ")
                append(declaration.name.asString())
                append(": ")
                append(declaration.type.render())
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

    override fun visitElement(element: IrElement, data: Nothing?): String = TODO()

    override fun visitDeclaration(declaration: IrDeclaration, data: Nothing?): String = TODO()

    override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): String = TODO()

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: Nothing?): String = TODO()

    override fun visitFile(declaration: IrFile, data: Nothing?): String = TODO()

    override fun visitFunction(declaration: IrFunction, data: Nothing?): String = TODO()

    override fun visitConstructor(declaration: IrConstructor, data: Nothing?): String =
        declaration.runTrimEnd {
            when (visibility) {
                Visibilities.PUBLIC -> ""
                else -> visibility.name.toLowerCase() + " "
            } + "constructor" + renderValueParameterTypes()
        }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?): String =
        declaration.runTrimEnd {
            renderSimpleFunctionFlags() +
                    (if (modality == Modality.FINAL) "" else modality.name.toLowerCase() + " ") +
                    (if (visibility == Visibilities.PUBLIC) "" else visibility.name.toLowerCase() + " ") +
                    "fun $name" +
                    renderTypeParameters() +
                    renderValueParameterTypes() +
                    ": ${returnType.toKotlinType()} "
        }

    private fun renderFlagsList(vararg flags: String?) =
        flags.filterNotNull().run {
            if (isNotEmpty())
                joinToString(separator = " ") + " "
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
        if (typeParameters.isEmpty())
            ""
        else
            typeParameters.joinToString(separator = ", ", prefix = "<", postfix = ">") { it.name.toString() }

    private fun IrFunction.renderValueParameterTypes(): String =
        ArrayList<String>().apply {
            valueParameters.mapTo(this) { "${it.name}: ${it.type.toKotlinType()}" }
        }.joinToString(separator = ", ", prefix = "(", postfix = ")")

    private fun IrConstructor.renderConstructorFlags() =
        renderFlagsList(
            "inline".takeIf { isInline },
            "external".takeIf { isExternal },
            "primary".takeIf { isPrimary }
        )

    override fun visitProperty(declaration: IrProperty, data: Nothing?): String = TODO()

    private fun IrProperty.renderPropertyFlags() =
        renderFlagsList(
            "external".takeIf { isExternal },
            "const".takeIf { isConst },
            "lateinit".takeIf { isLateinit },
            "delegated".takeIf { isDelegated },
            if (isVar) "var" else "val"
        )

    override fun visitField(declaration: IrField, data: Nothing?): String = TODO()


    private fun IrField.renderFieldFlags() =
        renderFlagsList(
            "final".takeIf { isFinal },
            "external".takeIf { isExternal },
            "static".takeIf { isStatic }
        )

    override fun visitClass(declaration: IrClass, data: Nothing?): String =
        buildTrimEnd {
            if (declaration.visibility != Visibilities.PUBLIC) append("${declaration.visibility.name.toLowerCase()} ")
            if (declaration.modality != Modality.FINAL && !declaration.isInterface) append("${declaration.modality.name.toLowerCase()} ")
            append(
                when {
                    declaration.isObject -> "object "
                    declaration.isInterface -> "interface "
                    declaration.isCompanion -> "companion object "
                    else -> "${declaration.renderClassFlags()}class "
                }
            )
            append(declaration.name.asString())
        }

    private fun IrClass.renderClassFlags() =
        renderFlagsList(
            "companion".takeIf { isCompanion },
            "inner".takeIf { isInner },
            "inline".takeIf { isInline },
            "data".takeIf { isData },
            "external".takeIf { isExternal },
            "annotation".takeIf { isAnnotationClass },
            "enum".takeIf { isEnumClass },
            "inline".takeIf { isInline }
        )

    override fun visitVariable(declaration: IrVariable, data: Nothing?): String = TODO()


    private fun IrVariable.renderVariableFlags(): String =
        renderFlagsList(
            "const".takeIf { isConst },
            "lateinit".takeIf { isLateinit },
            if (isVar) "var" else "val"
        )

    override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?): String = TODO()


    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?): String = TODO()

    override fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?): String = TODO()

    override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?): String = TODO()

    private fun IrValueParameter.renderValueParameterFlags(): String =
        renderFlagsList(
            "vararg".takeIf { varargElementType != null },
            "crossinline".takeIf { isCrossinline },
            "noinline".takeIf { isNoinline }
        )

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?): String = TODO()

    override fun visitTypeAlias(declaration: IrTypeAlias, data: Nothing?): String = TODO()

    private fun IrTypeAlias.renderTypeAliasFlags(): String =
        renderFlagsList(
            "actual".takeIf { isActual }
        )


    override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?): String = TODO()

    override fun visitBlockBody(body: IrBlockBody, data: Nothing?): String = TODO()

    override fun visitSyntheticBody(body: IrSyntheticBody, data: Nothing?): String = TODO()

    override fun visitExpression(expression: IrExpression, data: Nothing?): String = TODO()

    override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): String = TODO()
    private fun Any.escapeIfRequired() =
        when (this) {
            is String -> "\"${StringUtil.escapeStringCharacters(this)}\""
            is Char -> "'${StringUtil.escapeStringCharacters(this.toString())}'"
            else -> this
        }

    override fun visitVararg(expression: IrVararg, data: Nothing?): String = TODO()

    override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): String = TODO()

    override fun visitBlock(expression: IrBlock, data: Nothing?): String = TODO()

    override fun visitComposite(expression: IrComposite, data: Nothing?): String = TODO()

    override fun visitReturn(expression: IrReturn, data: Nothing?): String = "return"

    override fun visitCall(expression: IrCall, data: Nothing?): String =
        StringBuilder().apply {
            when (expression.origin) {
                IrStatementOrigin.UPLUS -> {
                    append("+")
                    append(expression.dispatchReceiver?.accept(this@DecompilerIrElementVisitor, null))
                }
                IrStatementOrigin.UMINUS -> {
                    append("-")
                    append(expression.dispatchReceiver?.accept(this@DecompilerIrElementVisitor, null))
                }
                IrStatementOrigin.PLUS -> {
                    append(expression.dispatchReceiver?.accept(this@DecompilerIrElementVisitor, null))
                    append(" + ")
                    append(expression.getValueArgument(0)?.accept(this@DecompilerIrElementVisitor, null))
                }
                IrStatementOrigin.MINUS -> {
                    append(expression.dispatchReceiver?.accept(this@DecompilerIrElementVisitor, null))
                    append(" - ")
                    append(expression.getValueArgument(0)?.accept(this@DecompilerIrElementVisitor, null))
                }
                IrStatementOrigin.MUL -> {
                    append(expression.dispatchReceiver?.accept(this@DecompilerIrElementVisitor, null))
                    append(" * ")
                    append(expression.getValueArgument(0)?.accept(this@DecompilerIrElementVisitor, null))
                }
                IrStatementOrigin.DIV -> {
                    append(expression.dispatchReceiver?.accept(this@DecompilerIrElementVisitor, null))
                    append(" / ")
                    append(expression.getValueArgument(0)?.accept(this@DecompilerIrElementVisitor, null))
                }
                IrStatementOrigin.PERC -> {
                    append(expression.dispatchReceiver?.accept(this@DecompilerIrElementVisitor, null))
                    append(" % ")
                    append(expression.getValueArgument(0)?.accept(this@DecompilerIrElementVisitor, null))
                }
                IrStatementOrigin.ANDAND -> {
                    append(expression.dispatchReceiver?.accept(this@DecompilerIrElementVisitor, null))
                    append(" && ")
                    append(expression.getValueArgument(0)?.accept(this@DecompilerIrElementVisitor, null))
                }
                IrStatementOrigin.OROR -> {
                    append(expression.dispatchReceiver?.accept(this@DecompilerIrElementVisitor, null))
                    append(" || ")
                    append(expression.getValueArgument(0)?.accept(this@DecompilerIrElementVisitor, null))
                }

                else -> {
                    if (expression.dispatchReceiver != null) {
                        append("${expression.dispatchReceiver?.accept(this@DecompilerIrElementVisitor, null)}.")
                    }
                    append(expression.symbol.owner.name.asString())
                    append(
                        ArrayList<String?>().apply {
                            (0 until expression.valueArgumentsCount).mapTo(this) {
                                expression.getValueArgument(it)?.accept(this@DecompilerIrElementVisitor, null)
                            }
                        }.filterNotNull().joinToString(separator = ", ", prefix = "(", postfix = ")")
                    )
                    append("\n")
                }
            }
        }.toString()


    override fun visitConstructorCall(expression: IrConstructorCall, data: Nothing?): String = TODO()

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?): String = TODO()

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?): String = TODO()

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: Nothing?): String = TODO()

    override fun visitGetValue(expression: IrGetValue, data: Nothing?): String = "${expression.symbol.owner.name}"

    override fun visitSetVariable(expression: IrSetVariable, data: Nothing?): String = TODO()

    override fun visitGetField(expression: IrGetField, data: Nothing?): String = TODO()

    override fun visitSetField(expression: IrSetField, data: Nothing?): String = TODO()

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?): String = TODO()

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?): String = TODO()

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): String = TODO()

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?): String = TODO()
    override fun visitWhen(expression: IrWhen, data: Nothing?): String = TODO()

    override fun visitBranch(branch: IrBranch, data: Nothing?): String = TODO()

    override fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?): String = TODO()

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Nothing?): String = TODO()

    override fun visitBreak(jump: IrBreak, data: Nothing?): String = TODO()

    override fun visitContinue(jump: IrContinue, data: Nothing?): String = TODO()

    override fun visitThrow(expression: IrThrow, data: Nothing?): String = TODO()

    override fun visitFunctionReference(expression: IrFunctionReference, data: Nothing?): String = TODO()

    override fun visitPropertyReference(expression: IrPropertyReference, data: Nothing?): String = TODO()


    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference, data: Nothing?): String = TODO()

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: Nothing?): String = TODO()

    override fun visitClassReference(expression: IrClassReference, data: Nothing?): String = TODO()

    override fun visitGetClass(expression: IrGetClass, data: Nothing?): String = TODO()

    override fun visitTry(aTry: IrTry, data: Nothing?): String = TODO()

    override fun visitCatch(aCatch: IrCatch, data: Nothing?): String = TODO()

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: Nothing?): String = TODO()

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: Nothing?): String = TODO()

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: Nothing?): String = TODO()

    override fun visitErrorExpression(expression: IrErrorExpression, data: Nothing?): String = TODO()

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: Nothing?): String = TODO()

}

internal fun IrDeclaration.name(): String =
    descriptor.name.toString()


internal fun IrDeclaration.renderOriginIfNonTrivial(): String =
    if (origin != IrDeclarationOrigin.DEFINED) "$origin " else ""

internal fun IrClassifierSymbol.renderClassifierFqn(): String =
    if (isBound)
        when (val owner = owner) {
            is IrClass -> owner.renderClassFqn()
            is IrTypeParameter -> owner.renderTypeParameterFqn()
            else -> "`unexpected classifier: ${owner.render()}`"
        }
    else
        "<unbound ${this.javaClass.simpleName}>"

internal fun IrTypeAliasSymbol.renderTypeAliasFqn(): String =
    if (isBound)
        StringBuilder().also { owner.renderDeclarationFqn(it) }.toString()
    else
        "<unbound $this: ${this.descriptor}>"

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

fun IrType.render() = DecompilerIrElementVisitor().renderType(this)

fun IrTypeArgument.render() =
    when (this) {
        is IrStarProjection -> "*"
        is IrTypeProjection -> "$variance ${type.render()}"
        else -> throw AssertionError("Unexpected IrTypeArgument: $this")
    }

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
