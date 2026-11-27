/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printKDoc
import org.jetbrains.kotlin.ir.generator.ExpressionBuilderConfigurator.constructorParameters
import org.jetbrains.kotlin.ir.generator.ExpressionBuilderConfigurator.contextValues
import org.jetbrains.kotlin.ir.generator.ExpressionBuilderConfigurator.hasAllValues
import org.jetbrains.kotlin.ir.generator.ExpressionBuilderConfigurator.nodeProperties
import org.jetbrains.kotlin.ir.generator.ExpressionBuilderConfigurator.properties
import org.jetbrains.kotlin.ir.generator.irFactoryType
import org.jetbrains.kotlin.ir.generator.model.*
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.utils.withIndent

/** Prints the v2 expression builders, see [org.jetbrains.kotlin.ir.generator.ExpressionBuilderConfigurator]. */
internal class ExpressionBuilderPrinter(private val printer: ImportCollectingPrinter) {
    private val irBuilderType = ArbitraryImportable("org.jetbrains.kotlin.ir.builders", "IrBuilder")
    private val irBuilderWithScopeType = ArbitraryImportable("org.jetbrains.kotlin.ir.builders", "IrBuilderWithScope")

    fun printBuilderWithFunctions(builder: ExpressionBuilder): Unit = with(printer) {
        printBuilder(builder)
        if (builder.isAbstract) return
        println()
        printBuildFunction(builder)
        if (builder.entryPoints.isNotEmpty()) {
            println()
            printEntryPoints(builder)
        }
    }

    private fun printBuilder(builder: ExpressionBuilder): Unit = with(printer) {
        printKDoc(builder.kDoc)
        for (annotation in builder.annotations) {
            printExpr(annotation)
            println()
        }
        when {
            builder.isAbstract -> print("abstract ")
            builder.isOpen -> print("open ")
        }
        print("class ", builder.typeName, builder.element.params.typeParameters())
        val parameters = if (builder.isAbstract) emptyList() else constructorParameters(builder)
        if (builder.isAbstract) {
            print(" internal constructor()")
        } else {
            print(" @PublishedApi internal constructor(")
            if (parameters.isNotEmpty()) {
                println()
                withIndent {
                    for (parameter in parameters) {
                        when {
                            parameter.isPassedToParent -> {}
                            parameter.isOverride -> print("override val ")
                            else -> print("val ")
                        }
                        println(parameter.field.name, ": ", parameter.field.typeRef.render(), ",")
                    }
                }
            }
            print(")")
        }
        builder.parent?.let { parent ->
            val args = builder.element.elementParents.singleOrNull { it.element == parent.element }?.args.orEmpty()
            print(" : ", parent.ref.copy(args).render(), "(")
            print(parameters.filter { it.isPassedToParent }.joinToString { it.field.name })
            print(")")
        }

        val members = mutableListOf<ImportCollectingPrinter.() -> Unit>()
        for (property in properties(builder)) {
            members += { printProperty(property) }
        }
        for (name in builder.initializationChecks) {
            members += {
                println("internal val is", name.replaceFirstChar(Char::uppercaseChar), "Initialized: Boolean")
                withIndent { println("get() = ::", name, ".isInitialized") }
            }
        }
        if (builder.initialValues.isNotEmpty() || builder.initHook != null) {
            members += {
                println("init {")
                withIndent {
                    for ([name, value] in builder.initialValues) {
                        print(name, " = ")
                        printExpr(value)
                        println()
                    }
                    builder.initHook?.let { println(it, "(this)") }
                }
                println("}")
            }
        }
        if (members.isEmpty()) {
            println()
            return
        }
        println(" {")
        withIndent {
            members.forEachIndexed { index, member ->
                if (index > 0) println()
                member()
            }
        }
        println("}")
    }

    private fun ImportCollectingPrinter.printProperty(property: ExpressionBuilderProperty) {
        val field = property.field
        val type = field.typeRef.render()
        when {
            property.isIdentity -> println("abstract ", if (property.isOverride) "override " else "", "val ", field.name, ": ", type)
            field is ListField && field.mutability == ListField.Mutability.MutableList -> println("val ", field.name, ": ", type, " = []")
            property.default == null -> println("lateinit var ", field.name, ": ", type)
            else -> {
                print("var ", field.name, ": ", type, " = ")
                printExpr(property.default)
                println()
            }
        }
    }

    /** `build(builder)`: creates the node with the implementation constructor (or the factory), then assigns the other properties. */
    private fun printBuildFunction(builder: ExpressionBuilder): Unit = with(printer) {
        val hierarchy = listOf(builder) + builder.ancestors
        val factoryMethod = builder.build.factoryMethod
        val beforeBuild = hierarchy.firstNotNullOfOrNull { it.build.beforeBuild }
        val afterBuild = hierarchy.firstNotNullOfOrNull { it.build.afterBuild }
        val setByHook = hierarchy.flatMapTo(mutableSetOf()) { it.build.setByHook }
        val implementation = builder.element.implementations.single()
        val constructorFields = implementation.fieldsInConstructor.map { it.name }
        val fields = nodeProperties(builder).filter { it.name !in setByHook }
        val missing = constructorFields - fields.mapTo(mutableSetOf()) { it.name }
        check(missing.isEmpty()) { "${builder.typeName}: no properties for the constructor parameters $missing of ${implementation.typeName}" }

        println("@PublishedApi")
        print("internal fun ")
        if (factoryMethod != null) print(irFactoryType.render(), ".")
        println("build(builder: ", builder.ref.render(), "): ", builder.element.render(), " {")
        withIndent {
            beforeBuild?.let { println(it, "(builder)") }
            print("val node = ")
            if (factoryMethod != null) {
                println(factoryMethod, "(")
            } else {
                println(implementation.render(), "(")
            }
            withIndent {
                if (factoryMethod == null) println("constructorIndicator = null,")
                for (name in constructorFields) {
                    println(name, " = builder.", name, ",")
                }
            }
            println(")")
            for (field in fields.filter { it.name !in constructorFields }) {
                when {
                    field is ListField && field.mutability == ListField.Mutability.MutableList ->
                        println("node.", field.name, ".addAll(builder.", field.name, ")")
                    field.isMutable -> println("node.", field.name, " = builder.", field.name)
                    else -> error("${builder.element.name}.${field.name} is neither a constructor parameter nor mutable")
                }
            }
            afterBuild?.let { println(it, "(builder, node)") }
            println("return node")
        }
        println("}")
    }

    /** The entry points of [builder]: they are inline, only the creation of the builder and `build` aren't. */
    private fun printEntryPoints(builder: ExpressionBuilder): Unit = with(printer) {
        val parameters = constructorParameters(builder)
        val declaredParameters = parameters.joinToString("") { "${it.field.name}: ${it.field.typeRef.render()}, " }
        val arguments = parameters.joinToString { it.field.name }
        val nodeType = builder.element.render()
        val builderType = builder.ref.render()
        val creatorName = builder.nodeName.replaceFirstChar(Char::lowercaseChar) + "Builder"
        var isFirst = true
        fun separate() {
            if (!isFirst) println()
            isFirst = false
        }
        fun lambdaParameter(entryPoint: ExpressionEntryPoint): String {
            val default = if (hasAllValues(builder, entryPoint)) " = {}" else ""
            return "builder: $builderType.() -> Unit$default"
        }

        for (entryPoint in builder.entryPoints) {
            separate()
            when (entryPoint) {
                ExpressionEntryPoint.ContextFree -> {
                    print("inline fun build", builder.nodeName, "(", declaredParameters, lambdaParameter(entryPoint))
                    println("): ", nodeType, " {")
                    printEntryPointBody { println("return build(", builderType, "(", arguments, ").apply(builder))") }
                }
                ExpressionEntryPoint.IrFactory -> {
                    check(parameters.isEmpty())
                    val factory = irFactoryType.render()
                    println("inline fun ", factory, ".ir", builder.nodeName, "(", lambdaParameter(entryPoint), "): ", nodeType, " {")
                    printEntryPointBody { println("return build(", builderType, "().apply(builder))") }
                }
                ExpressionEntryPoint.IrBuilder, ExpressionEntryPoint.IrBuilderWithScope -> {
                    val receiver = if (entryPoint == ExpressionEntryPoint.IrBuilder) irBuilderType else irBuilderWithScopeType
                    addImport(receiver)
                    val receiverName = receiver.typeName
                    println(
                        "inline fun ", receiverName, ".ir", builder.nodeName, "(", declaredParameters, lambdaParameter(entryPoint), "): ",
                        nodeType, " {"
                    )
                    printEntryPointBody { println("return build(", creatorName, "(", arguments, ").apply(builder))") }
                    println()
                    println("@PublishedApi")
                    print("internal fun ", receiverName, ".", creatorName, "(", declaredParameters.removeSuffix(", "))
                    println("): ", builderType, " =")
                    withIndent {
                        println(builderType, "(", arguments, ").also {")
                        withIndent {
                            println("it.startOffset = startOffset")
                            println("it.endOffset = endOffset")
                            for ([name, value] in contextValues(builder)) {
                                print("it.", name, " = ")
                                printExpr(value)
                                println()
                            }
                            builder.contextHook?.let { println(it, "(it)") }
                        }
                        println("}")
                    }
                }
            }
        }
    }

    private inline fun ImportCollectingPrinter.printEntryPointBody(printReturn: () -> Unit) {
        withIndent {
            printCallsInPlaceContract("builder")
            printReturn()
        }
        println("}")
    }

    private fun ImportCollectingPrinter.printExpr(expr: Expr) {
        addAllImports(expr.imports)
        print(expr.text)
    }
}
