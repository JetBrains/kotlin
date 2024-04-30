/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree.printer

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.IndentingPrinter
import org.jetbrains.kotlin.utils.withIndent

interface ImportCollectingPrinter : ImportCollecting, IndentingPrinter

/**
 * The braces to print in the inheritance clause if this is a class, or empty string if this is an interface.
 */
fun ImplementationKind?.braces(): String = when (this) {
    ImplementationKind.Interface, ImplementationKind.SealedInterface -> ""
    ImplementationKind.OpenClass, ImplementationKind.AbstractClass, ImplementationKind.SealedClass -> "()"
    else -> throw IllegalStateException(this.toString())
}

fun IndentingPrinter.printKDoc(kDoc: String?) {
    if (kDoc == null) return
    println("/**")
    for (line in kDoc.lineSequence()) {
        print(" *")
        if (line.isBlank()) {
            println()
        } else {
            print(" ")
            println(line)
        }
    }
    println(" */")
}

fun AbstractElement<*, *, *>.extendedKDoc(): String = buildString {
    val doc = kDoc
    if (doc != null) {
        appendLine(doc)
        appendLine()
    }
    append("Generated from: [${element.propertyName}]")
}

data class FunctionParameter(val name: String, val type: TypeRef, val defaultValue: String? = null) {

    fun render(importCollector: ImportCollecting): String = buildString {
        append(name, ": ")
        type.renderTo(this, importCollector)
        defaultValue?.let {
            append(" = ", it)
        }
    }
}

fun ImportCollectingPrinter.printFunctionDeclaration(
    name: String,
    parameters: List<FunctionParameter>,
    returnType: TypeRef,
    typeParameters: List<TypeVariable> = emptyList(),
    extensionReceiver: TypeRef? = null,
    visibility: Visibility = Visibility.PUBLIC,
    modality: Modality? = null,
    override: Boolean = false,
    isInline: Boolean = false,
    allParametersOnSeparateLines: Boolean = false,
    optInAnnotation: ClassRef<*>? = null,
    deprecation: Deprecated? = null,
) {
    optInAnnotation?.let {
        println("@", it.render())
    }

    deprecation?.let {
        printDeprecation(it)
    }

    if (visibility != Visibility.PUBLIC) {
        print(visibility.name.toLowerCaseAsciiOnly(), " ")
    }
    when (modality) {
        null -> {}
        Modality.FINAL -> print("final ")
        Modality.OPEN -> print("open ")
        Modality.ABSTRACT -> print("abstract ")
        Modality.SEALED -> error("Function cannot be sealed")
    }
    if (override) {
        print("override ")
    }
    if (isInline) {
        print("inline ")
    }
    print("fun ")
    print(typeParameters.typeParameters(end = " "))
    if (extensionReceiver != null) {
        print(extensionReceiver.render(), ".")
    }
    print(name, "(")

    if (allParametersOnSeparateLines) {
        if (parameters.isNotEmpty()) {
            println()
            withIndent {
                for (parameter in parameters) {
                    print(parameter.render(this))
                    println(",")
                }
            }
        }
    } else {
        print(parameters.joinToString { it.render(this) })
    }
    print(")")
    if (returnType != StandardTypes.unit) {
        print(": ", returnType.render())
    }
    print(typeParameters.multipleUpperBoundsList())
}

inline fun ImportCollectingPrinter.printFunctionWithBlockBody(
    name: String,
    parameters: List<FunctionParameter>,
    returnType: TypeRef,
    typeParameters: List<TypeVariable> = emptyList(),
    extensionReceiver: TypeRef? = null,
    visibility: Visibility = Visibility.PUBLIC,
    modality: Modality? = null,
    override: Boolean = false,
    isInline: Boolean = false,
    allParametersOnSeparateLines: Boolean = false,
    deprecation: Deprecated? = null,
    blockBody: () -> Unit,
) {
    printFunctionDeclaration(
        name,
        parameters,
        returnType,
        typeParameters,
        extensionReceiver,
        visibility,
        modality,
        override,
        isInline,
        allParametersOnSeparateLines,
        deprecation = deprecation,
    )
    printBlock(blockBody)
}

private fun IndentingPrinter.printDeprecation(deprecation: Deprecated) {
    println("@Deprecated(")
    withIndent {
        println("message = \"", deprecation.message, "\",")
        println("replaceWith = ReplaceWith(\"", deprecation.replaceWith.expression, "\"),")
        println("level = DeprecationLevel.", deprecation.level.name, ",")
    }
    println(")")
}


fun ImportCollectingPrinter.printPropertyDeclaration(
    name: String,
    type: TypeRef,
    kind: VariableKind,
    inConstructor: Boolean = false,
    visibility: Visibility = Visibility.PUBLIC,
    modality: Modality? = null,
    override: Boolean = false,
    isLateinit: Boolean = false,
    isVolatile: Boolean = false,
    kDoc: String? = null,
    optInAnnotation: ClassRef<*>? = null,
    printOptInWrapped: Boolean = false,
    deprecation: Deprecated? = null,
    initializer: String? = null,
) {
    printKDoc(kDoc)

    deprecation?.let {
        printDeprecation(it)
    }

    if (isVolatile) {
        println("@", type<Volatile>().render())
    }

    optInAnnotation?.let {
        val rendered = it.render()
        when {
            printOptInWrapped -> println("@OptIn(", rendered, "::class)")
            inConstructor -> println("@property:", rendered)
            else -> println("@", rendered)
        }
    }

    if (visibility != Visibility.PUBLIC) {
        print(visibility.name.toLowerCaseAsciiOnly(), " ")
    }

    modality?.let {
        print(it.name.toLowerCaseAsciiOnly(), " ")
    }

    if (override) {
        print("override ")
    }
    if (isLateinit) {
        print("lateinit ")
    }
    when (kind) {
        VariableKind.PARAMETER -> {}
        VariableKind.VAL -> print("val ")
        VariableKind.VAR -> print("var ")
    }
    print(name, ": ", type.render())

    if (initializer != null) {
        print(" = $initializer")
    }

    if (inConstructor) {
        print(",")
    }
}

enum class VariableKind { VAL, VAR, PARAMETER }

inline fun IndentingPrinter.printBlock(body: () -> Unit) {
    println(" {")
    withIndent(body)
    println("}")
}

private val dataTP = TypeVariable("D")
private val dataParameter = FunctionParameter("data", dataTP)

private fun acceptMethodKDoc(
    visitorParameter: FunctionParameter,
    dataParameter: FunctionParameter?,
    returnType: TypeRef,
    treeName: String,
) = buildString {
    append("Runs the provided [")
    append(visitorParameter.name)
    append("] on the ")
    append(treeName)
    append(" subtree with the root at this node.\n\n")
    append("@param ")
    append(visitorParameter.name)
    append(" The visitor to accept.")
    if (dataParameter != null) {
        append("\n@param ")
        append(dataParameter.name)
        append(" An arbitrary context to pass to each invocation of [")
        append(visitorParameter.name)
        append("]'s methods.")
    }
    if (returnType != StandardTypes.unit) {
        append("\n@return The value returned by the topmost `visit*` invocation.")
    }
}

fun ImportCollectingPrinter.printAcceptMethod(
    element: AbstractElement<*, *, *>,
    visitorClass: ClassRef<PositionTypeParameterRef>,
    hasImplementation: Boolean,
    treeName: String,
) {
    if (!element.hasAcceptMethod) return
    println()
    val resultTP = TypeVariable("R")
    val visitorParameter = FunctionParameter("visitor", visitorClass.withArgs(resultTP, dataTP))
    if (element.isRootElement) {
        printKDoc(acceptMethodKDoc(visitorParameter, dataParameter, resultTP, treeName))
    }
    printFunctionDeclaration(
        name = "accept",
        parameters = listOf(visitorParameter, dataParameter),
        returnType = resultTP,
        typeParameters = listOf(resultTP, dataTP),
        override = !element.isRootElement,
    )
    if (hasImplementation) {
        println(" =")
        withIndent {
            print(visitorParameter.name, ".", element.visitFunctionName, "(this, ", dataParameter.name, ")")
        }
    }
    println()
}

private fun transformMethodKDoc(
    transformerParameter: FunctionParameter,
    dataParameter: FunctionParameter?,
    treeName: String,
) = buildString {
    append("Runs the provided [")
    append(transformerParameter.name)
    append("] on the $treeName subtree with the root at this node.\n\n")
    append("@param ")
    append(transformerParameter.name)
    append(" The transformer to use.")
    if (dataParameter != null) {
        append("\n@param ")
        append(dataParameter.name)
        append(" An arbitrary context to pass to each invocation of [")
        append(transformerParameter.name)
        append("]'s methods.")
    }
    append("\n@return The transformed node.")
}

fun ImportCollectingPrinter.printTransformMethod(
    element: AbstractElement<*, *, *>,
    transformerClass: ClassRef<PositionTypeParameterRef>,
    implementation: String?,
    returnType: TypeRefWithNullability,
    treeName: String,
) {
    if (!element.hasTransformMethod) return
    println()
    val transformerParameter = FunctionParameter("transformer", transformerClass.withArgs(dataTP))
    if (element.isRootElement) {
        printKDoc(transformMethodKDoc(transformerParameter, dataParameter, treeName))
    }
    if (returnType is TypeParameterRef && implementation != null) {
        println("@Suppress(\"UNCHECKED_CAST\")")
    }
    printFunctionDeclaration(
        name = "transform",
        parameters = listOf(transformerParameter, dataParameter),
        returnType = returnType,
        typeParameters = listOfNotNull(returnType as? TypeVariable, dataTP),
        override = !element.isRootElement,
    )
    if (implementation != null) {
        println(" =")
        withIndent {
            print(implementation, " as ", returnType.render())
        }
    }
    println()
}

private fun acceptChildrenKDoc(visitorParameter: FunctionParameter, dataParameter: FunctionParameter?) = buildString {
    append("Runs the provided [")
    append(visitorParameter.name)
    append("] on subtrees with roots in this node's children.\n\n")
    append("Basically, calls `accept(")
    append(visitorParameter.name)
    if (dataParameter != null) {
        append(", ")
        append(dataParameter.name)
    }
    append(")` on each child of this node.\n\n")
    append("Does **not** run [")
    append(visitorParameter.name)
    append("] on this node itself.\n\n")
    append("@param ")
    append(visitorParameter.name)
    append(" The visitor for children to accept.")
    if (dataParameter != null) {
        append("\n@param ")
        append(dataParameter.name)
        append(" An arbitrary context to pass to each invocation of [")
        append(visitorParameter.name)
        append("]'s methods.")
    }
}

fun ImportCollectingPrinter.printAcceptChildrenMethod(
    element: FieldContainer<*>,
    visitorClass: ClassRef<PositionTypeParameterRef>,
    visitorResultType: TypeRef,
    modality: Modality? = null,
    override: Boolean = false,
) {
    if (!element.hasAcceptChildrenMethod) return
    println()
    val visitorParameter = FunctionParameter("visitor", visitorClass.withArgs(visitorResultType, dataTP))
    if (!override) {
        printKDoc(acceptChildrenKDoc(visitorParameter, dataParameter))
    }
    printFunctionDeclaration(
        name = "acceptChildren",
        parameters = listOf(visitorParameter, dataParameter),
        returnType = StandardTypes.unit,
        typeParameters = listOfNotNull(visitorResultType as? TypeVariable, dataTP),
        modality = modality,
        override = override,
    )
}

private fun transformChildrenMethodKDoc(transformerParameter: FunctionParameter, dataParameter: FunctionParameter?, returnType: TypeRef) =
    buildString {
        append("Recursively transforms this node's children *in place* using [")
        append(transformerParameter.name)
        append("].\n\n")
        append("Basically, executes `this.child = this.child.transform(")
        append(transformerParameter.name)
        if (dataParameter != null) {
            append(", ")
            append(dataParameter.name)
        }
        append(")` for each child of this node.\n\n")
        append("Does **not** run [")
        append(transformerParameter.name)
        append("] on this node itself.\n\n")
        append("@param ")
        append(transformerParameter.name)
        append(" The transformer to use for transforming the children.")
        if (dataParameter != null) {
            append("\n@param ")
            append(dataParameter.name)
            append(" An arbitrary context to pass to each invocation of [")
            append(transformerParameter.name)
            append("]'s methods.")
        }
        if (returnType != StandardTypes.unit) {
            append("\n@return `this`")
        }
    }

fun ImportCollectingPrinter.printTransformChildrenMethod(
    element: FieldContainer<*>,
    transformerClass: ClassRef<PositionTypeParameterRef>,
    returnType: TypeRef,
    modality: Modality? = null,
    override: Boolean = false,
) {
    if (!element.hasTransformChildrenMethod) return
    println()
    val transformerParameter = FunctionParameter("transformer", transformerClass.withArgs(dataTP))
    if (!override) {
        printKDoc(transformChildrenMethodKDoc(transformerParameter, dataParameter, returnType))
    }
    printFunctionDeclaration(
        name = "transformChildren",
        parameters = listOf(transformerParameter, dataParameter),
        returnType = returnType,
        typeParameters = listOf(dataTP),
        modality = modality,
        override = override,
    )
}

fun ImportCollectingPrinter.printAcceptVoidMethod(visitorType: ClassRef<*>, treeName: String) {
    val visitorParameter = FunctionParameter("visitor", visitorType)
    val returnType = StandardTypes.unit
    printKDoc(acceptMethodKDoc(visitorParameter, null, returnType, treeName))
    printFunctionDeclaration("accept", listOf(visitorParameter), returnType)
    printBlock {
        println("accept(", visitorParameter.name, ", null)")
    }
}

fun ImportCollectingPrinter.printAcceptChildrenVoidMethod(visitorType: ClassRef<*>) {
    val visitorParameter = FunctionParameter("visitor", visitorType)
    printKDoc(acceptChildrenKDoc(visitorParameter, null))
    printFunctionDeclaration("acceptChildren", listOf(visitorParameter), StandardTypes.unit)
    printBlock {
        println("acceptChildren(", visitorParameter.name, ", null)")
    }
}

fun ImportCollectingPrinter.printTransformVoidMethod(element: AbstractElement<*, *, *>, transformerType: ClassRef<*>, treeName: String) {
    assert(element.isRootElement) { "Expected root element" }
    val transformerParameter = FunctionParameter("transformer", transformerType)
    val elementTP = TypeVariable("E", listOf(element))
    printKDoc(transformMethodKDoc(transformerParameter, null, treeName))
    printFunctionDeclaration(
        name = "transform",
        parameters = listOf(transformerParameter),
        returnType = elementTP,
        typeParameters = listOf(elementTP)
    )
    println(" =")
    withIndent {
        println("transform(", transformerParameter.name, ", null)")
    }
}

fun ImportCollectingPrinter.printTransformChildrenVoidMethod(element: AbstractElement<*, *, *>, visitorType: ClassRef<*>, returnType: TypeRef) {
    assert(element.isRootElement) { "Expected root element" }
    val transformerParameter = FunctionParameter("transformer", visitorType)
    printKDoc(transformChildrenMethodKDoc(transformerParameter, null, returnType))
    printFunctionDeclaration("transformChildren", listOf(transformerParameter), returnType)
    println(" =")
    withIndent {
        println("transformChildren(", transformerParameter.name, ", null)")
    }
}

fun AbstractField<*>.call(): String = if (nullable) "?." else "."
