/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree.printer

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.generators.tree.ImportCollector
import org.jetbrains.kotlin.generators.tree.render
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.joinToWithBuffer
import org.jetbrains.kotlin.utils.withIndent

/**
 * The angle bracket-delimited list of type parameters to print, or empty string if the list is empty.
 *
 * For type parameters that have a single upper bound, also prints that upper bound. If at least one type parameter has multiple upper
 * bounds, doesn't print any upper bounds at all. They are expected to be printed in the `where` clause (see [multipleUpperBoundsList]).
 *
 * @param end The string to add after the closing angle bracket of the type parameter list
 */
context(ImportCollector)
fun List<TypeVariable>.typeParameters(end: String = ""): String = buildString {
    if (this@typeParameters.isEmpty()) return@buildString
    joinToWithBuffer(this, prefix = "<", postfix = ">") { param ->
        if (param.variance != Variance.INVARIANT) {
            append(param.variance.label)
            append(" ")
        }
        append(param.name)
        param.bounds.singleOrNull()?.let {
            append(" : ")
            it.renderTo(this)
        }
    }
    append(end)
}

/**
 * The `where` clause to print after the class or function declaration if at least one of the element's tye parameters has multiple upper
 * bounds.
 *
 * Otherwise, an empty string.
 */
context(ImportCollector)
fun List<TypeVariable>.multipleUpperBoundsList(): String {
    val paramsWithMultipleUpperBounds = filter { it.bounds.size > 1 }.takeIf { it.isNotEmpty() } ?: return ""
    return buildString {
        append(" where ")
        paramsWithMultipleUpperBounds.joinToWithBuffer(this, separator = ", ") { param ->
            param.bounds.joinToWithBuffer(this) { bound ->
                append(param.name)
                append(" : ")
                bound.renderTo(this)
            }
        }
    }
}

/**
 * The braces to print in the inheritance clause if this is a class, or empty string if this is an interface.
 */
fun ImplementationKind?.braces(): String = when (this) {
    ImplementationKind.Interface, ImplementationKind.SealedInterface -> ""
    ImplementationKind.OpenClass, ImplementationKind.AbstractClass, ImplementationKind.SealedClass -> "()"
    else -> throw IllegalStateException(this.toString())
}

fun SmartPrinter.printKDoc(kDoc: String?) {
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

fun AbstractElement<*, *>.extendedKDoc(defaultKDoc: String? = null): String = buildString {
    val doc = kDoc ?: defaultKDoc
    if (doc != null) {
        appendLine(doc)
        appendLine()
    }
    append("Generated from: [${element.propertyName}]")
}

data class FunctionParameter(val name: String, val type: TypeRef, val defaultValue: String? = null) {

    context(ImportCollector)
    fun render(): String = buildString {
        append(name, ": ", type.render())
        defaultValue?.let {
            append(" = ", it)
        }
    }
}

context(ImportCollector)
fun SmartPrinter.printFunctionDeclaration(
    name: String,
    parameters: List<FunctionParameter>,
    returnType: TypeRef,
    typeParameters: List<TypeVariable> = emptyList(),
    modality: Modality? = null,
    override: Boolean = false,
    allParametersOnSeparateLines: Boolean = false,
) {
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
    print("fun ")
    print(typeParameters.typeParameters(end = " "))
    print(name, "(")

    if (allParametersOnSeparateLines) {
        if (parameters.isNotEmpty()) {
            println()
            withIndent {
                for (parameter in parameters) {
                    print(parameter.render())
                    println(",")
                }
            }
        }
    } else {
        print(parameters.joinToString { it.render() })
    }
    print(")")
    if (returnType != StandardTypes.unit) {
        print(": ", returnType.render())
    }
    print(typeParameters.multipleUpperBoundsList())
}

private val dataTP = TypeVariable("D")
private val dataParameter = FunctionParameter("data", dataTP)

context(ImportCollector)
fun SmartPrinter.printAcceptMethod(
    element: AbstractElement<*, *>,
    visitorClass: ClassRef<PositionTypeParameterRef>,
    hasImplementation: Boolean,
    treeName: String,
) {
    if (!element.hasAcceptMethod) return
    println()
    val resultTP = TypeVariable("R")
    val visitorParameter = FunctionParameter("visitor", visitorClass.withArgs(resultTP, dataTP))
    if (element.isRootElement) {
        printKDoc(
            """
            Runs the provided [${visitorParameter.name}] on the $treeName subtree with the root at this node.
            
            @param ${visitorParameter.name} The visitor to accept.
            @param ${dataParameter.name} An arbitrary context to pass to each invocation of [${visitorParameter.name}]'s methods.
            @return The value returned by the topmost `visit*` invocation.
            """.trimIndent()
        )
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

context(ImportCollector)
fun SmartPrinter.printTransformMethod(
    element: AbstractElement<*, *>,
    transformerClass: ClassRef<PositionTypeParameterRef>,
    implementation: String?,
    returnType: TypeRefWithNullability,
    treeName: String,
) {
    if (!element.hasTransformMethod) return
    println()
    val transformerParameter = FunctionParameter("transformer", transformerClass.withArgs(dataTP))
    if (element.isRootElement) {
        printKDoc(
            """
            Runs the provided [${transformerParameter.name}] on the $treeName subtree with the root at this node.
            
            @param ${transformerParameter.name} The transformer to use.
            @param ${dataParameter.name} An arbitrary context to pass to each invocation of [${transformerParameter.name}]'s methods.
            @return The transformed node.
            """.trimIndent()
        )
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

context(ImportCollector)
fun SmartPrinter.printAcceptChildrenMethod(
    element: FieldContainer,
    visitorClass: ClassRef<PositionTypeParameterRef>,
    visitorResultType: TypeRef,
    modality: Modality? = null,
    override: Boolean = false,
) {
    if (!element.hasAcceptChildrenMethod) return
    println()
    val visitorParameter = FunctionParameter("visitor", visitorClass.withArgs(visitorResultType, dataTP))
    if (!override) {
        printKDoc(
            """
            Runs the provided [${visitorParameter.name}] on subtrees with roots in this node's children.
            
            Basically, calls `accept(${visitorParameter.name}, ${dataParameter.name})` on each child of this node.
            
            Does **not** run [${visitorParameter.name}] on this node itself.
            
            @param ${visitorParameter.name} The visitor for children to accept.
            @param ${dataParameter.name} An arbitrary context to pass to each invocation of [${visitorParameter.name}]'s methods.
            """.trimIndent()
        )
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

context(ImportCollector)
fun SmartPrinter.printTransformChildrenMethod(
    element: FieldContainer,
    transformerClass: ClassRef<PositionTypeParameterRef>,
    returnType: TypeRef,
    modality: Modality? = null,
    override: Boolean = false,
) {
    if (!element.hasTransformChildrenMethod) return
    println()
    val transformerParameter = FunctionParameter("transformer", transformerClass.withArgs(dataTP))
    if (!override) {
        printKDoc(
            """
            Recursively transforms this node's children *in place* using [${transformerParameter.name}].
            
            Basically, executes `this.child = this.child.transform(${transformerParameter.name}, ${dataParameter.name})` for each child of this node.
            
            Does **not** run [${transformerParameter.name}] on this node itself.
            
            @param ${transformerParameter.name} The transformer to use for transforming the children.
            @param ${dataParameter.name} An arbitrary context to pass to each invocation of [${transformerParameter.name}]'s methods.
            """.trimIndent() + (if (returnType == StandardTypes.unit) "" else "\n@return `this`")
        )
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
