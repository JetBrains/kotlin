/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.ClassRef
import org.jetbrains.kotlin.generators.tree.ElementOrRef
import org.jetbrains.kotlin.generators.tree.TypeKind
import org.jetbrains.kotlin.generators.tree.TypeRef
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printKDoc
import org.jetbrains.kotlin.generators.tree.type
import org.jetbrains.kotlin.ir.generator.DeclarationBuilderConfigurator.copiedProperties
import org.jetbrains.kotlin.ir.generator.DeclarationBuilderConfigurator.headerProperties
import org.jetbrains.kotlin.ir.generator.DeclarationBuilderConfigurator.headerPropertiesOfNode
import org.jetbrains.kotlin.ir.generator.Packages
import org.jetbrains.kotlin.ir.generator.irFactoryType
import org.jetbrains.kotlin.ir.generator.model.*
import org.jetbrains.kotlin.utils.withIndent

/** Prints the classes of the v2 declaration builders, see [org.jetbrains.kotlin.ir.generator.DeclarationBuilderConfigurator]. */
internal class DeclarationBuilderPrinter(private val printer: ImportCollectingPrinter) {

    fun printBuilderWithFunctions(builder: DeclarationBuilder): Unit = with(printer) {
        printBuilder(builder)
        if (builder.build != null) {
            println()
            printBuildFunction(builder)
        }
        if (builder.entryPoints.isNotEmpty() || builder.creators.isNotEmpty()) {
            println()
            printEntryPoints(builder)
        }
    }

    private fun printBuilder(builder: DeclarationBuilder): Unit = with(printer) {
        printKDoc(builder.kDoc)
        for (annotation in builder.annotations) {
            printExpr(annotation)
            println()
        }
        if (builder.isAbstract) print("abstract ")
        print("class ", builder.typeName)
        if (builder.isCreatedByEntryPoint) print(" @PublishedApi")
        print(" internal constructor()")
        builder.parent?.let { print(" : ", it.ref.render(), "()") }
        println(" {")
        withIndent {
            val members = mutableListOf<ImportCollectingPrinter.() -> Unit>()
            val properties = headerProperties(builder)
            for (property in properties) {
                members += { printHeaderProperty(property) }
            }
            for (extra in builder.extraProperties) {
                members += {
                    print("var ", extra.name, ": ", extra.type.render(), " = ")
                    printExpr(extra.initializer)
                    println()
                }
            }
            for (override in builder.fieldOverrides) {
                members += { printFieldOverride(builder, override) }
            }
            if (builder.preallocatesSymbol) {
                members += {
                    println("internal val symbol: ", symbolType(builder).render(), " = ", symbolImplType(builder).render(), "()")
                }
            }
            if (builder.children.isNotEmpty()) {
                members += {
                    for (child in builder.children) {
                        printChildStorage(child)
                    }
                }
            }
            for (name in builder.initializationChecks) {
                val property = properties.singleOrNull { it.field.name == name }
                check(property != null && property.default == null && !property.isAbstract) {
                    "${builder.typeName}: $name isn't a lateinit property of the builder"
                }
                members += {
                    println("internal val is", name.replaceFirstChar(Char::uppercaseChar), "Initialized: Boolean")
                    withIndent {
                        println("get() = ::", name, ".isInitialized")
                    }
                }
            }
            if (builder.initialValues.isNotEmpty()) {
                members += {
                    println("init {")
                    withIndent {
                        for ([name, value] in builder.initialValues) {
                            print(name, " = ")
                            printExpr(value)
                            println()
                        }
                    }
                    println("}")
                }
            }
            val copied = copiedProperties(builder)
            if (copied.isNotEmpty()) {
                members += { printCopyFrom(builder, copied) }
            }
            for (child in builder.children) {
                when (child) {
                    is Child.Built -> child.functions.forEach { function -> members += { printChildFunction(child, function) } }
                    is Child.Nodes -> members += { printNodesFunction(child) }
                }
            }
            for (member in builder.additionalMembers) {
                members += {
                    addAllImports(member.imports)
                    printlnMultiLine(member.text)
                }
            }
            members.forEachIndexed { index, member ->
                if (index > 0) println()
                member()
            }
        }
        println("}")
    }

    private fun ImportCollectingPrinter.printCopyFrom(builder: DeclarationBuilder, properties: List<HeaderProperty>) {
        printKDoc("Copies the header of [from] as is, without relations to other declarations, metadata, annotations and children.")
        println("fun copyFrom(from: ", builder.element.render(), ") {")
        withIndent {
            for (property in properties) {
                println(property.field.name, " = from.", property.field.name)
            }
        }
        println("}")
    }

    private fun ImportCollectingPrinter.printExpr(expr: Expr) {
        addAllImports(expr.imports)
        print(expr.text)
    }

    private fun ImportCollectingPrinter.printHeaderProperty(property: HeaderProperty) {
        val name = property.field.name
        val type = property.type.render()
        when {
            property.isAbstract -> println("abstract val ", name, ": ", type)
            property.default == null -> println("lateinit var ", name, ": ", type)
            else -> {
                print("var ", name, ": ", type, " = ")
                printExpr(property.default)
                println()
            }
        }
    }

    private fun ImportCollectingPrinter.printFieldOverride(builder: DeclarationBuilder, override: FieldOverride) {
        val property = builder.ancestors.flatMap { headerProperties(it) }.single { it.field.name == override.name }
        check(property.isAbstract) { "${builder.typeName}: ${override.name} isn't abstract in the parents" }
        val type = property.type.render()
        if (override.isMutable) {
            print("override var ", override.name, ": ", type, " = ")
            printExpr(override.value)
            println()
        } else {
            println("override val ", override.name, ": ", type)
            withIndent {
                print("get() = ")
                printExpr(override.value)
                println()
            }
        }
    }

    private fun ImportCollectingPrinter.printChildStorage(child: Child) {
        when (child) {
            is Child.Built -> if (child.isList) {
                println("internal val ", child.propertyName, ": MutableList<", child.storedType.ref.render(), "> = []")
            } else {
                println("internal var ", child.propertyName, ": ", child.storedType.ref.render(), "? = null")
            }
            is Child.Nodes ->
                println("internal val ", child.propertyName, ": MutableList<", child.nodeType.render(), "> = []")
        }
    }

    private fun ImportCollectingPrinter.printChildFunction(child: Child.Built, function: ChildFunction) {
        printKDoc(function.kDoc)
        print("fun ", function.name, "(builder: ", function.builder.ref.render(), ".() -> Unit")
        if (function.hasDefaultLambda) print(" = {}")
        print(")")
        if (function.returnsSymbol) {
            check(function.builder.hasPreallocatedSymbol) { "${function.builder.typeName} has no preallocated symbol to return" }
            print(": ", symbolType(function.builder).render())
        }
        println(" {")
        withIndent {
            printCallsInPlaceContract("builder")
            function.alreadyDeclaredMessage?.let { message ->
                check(!child.isList)
                println("check(", child.propertyName, " == null) { \"", message, "\" }")
            }
            print("val child = ")
            check(function.creator?.receiver == null) { "${function.name}: a child builder can't be created by a function with a receiver" }
            println(creation(function.builder, function.creator), ".apply(builder)")
            if (child.isList) {
                println(child.propertyName, " += child")
            } else {
                println(child.propertyName, " = child")
            }
            if (function.returnsSymbol) {
                println("return child.symbol")
            }
        }
        println("}")
    }

    private fun ImportCollectingPrinter.printNodesFunction(child: Child.Nodes) {
        val parameterName = child.functionName
        println("fun ", child.functionName, "(", parameterName, ": ", child.nodeType.render(), ") {")
        withIndent {
            println(child.propertyName, " += ", parameterName)
        }
        println("}")
    }

    /** The entry points of [builder] and the functions creating it with defaults, see [DeclarationEntryPoint] and [BuilderCreator]. */
    private fun printEntryPoints(builder: DeclarationBuilder): Unit = with(printer) {
        val builderType = builder.ref.render()
        val nodeType = builder.element.render()
        val members = mutableListOf<ImportCollectingPrinter.() -> Unit>()
        for (entryPoint in builder.entryPoints) {
            members += {
                printKDoc(entryPoint.kDoc)
                print("inline fun ", entryPoint.receiver.render(), ".", entryPoint.name, "(builder: ", builderType, ".() -> Unit")
                if (entryPoint.hasDefaultLambda) print(" = {}")
                println("): ", nodeType, " {")
                withIndent {
                    printCallsInPlaceContract("builder")
                    println("return ", entryPoint.build, "(", creation(builder, entryPoint.creator), ".apply(builder))")
                }
                println("}")
            }
        }
        for (creator in builder.creators) {
            members += {
                println("@PublishedApi")
                print("internal fun ")
                creator.receiver?.let { print(it.render(), ".") }
                println(creator.name, "(): ", builderType, " =")
                withIndent {
                    println(builderType, "().also {")
                    withIndent {
                        if (creator.offsetsFromReceiver) {
                            check(creator.receiver != null) { "${creator.name} has no receiver to take the offsets from" }
                            println("it.startOffset = startOffset")
                            println("it.endOffset = endOffset")
                        }
                        for ([name, value] in creator.values) {
                            print("it.", name, " = ")
                            printExpr(value)
                            println()
                        }
                    }
                    println("}")
                }
            }
        }
        members.forEachIndexed { index, member ->
            if (index > 0) println()
            member()
        }
    }

    private fun ImportCollectingPrinter.creation(builder: DeclarationBuilder, creator: BuilderCreator?): String {
        if (creator == null) return "${builder.ref.render()}()"
        check(creator in builder.creators) { "${creator.name} isn't a creator of ${builder.typeName}" }
        return "${creator.name}()"
    }

    /** `IrFactory.build(builder)`: creates the node with its header, then calls the hooks of [BuildConfig]. */
    private fun printBuildFunction(builder: DeclarationBuilder): Unit = with(printer) {
        val config = builder.build ?: return
        val implementation = builder.element.implementations.single()
        val constructorFields = implementation.fieldsInConstructor.mapTo(mutableSetOf()) { it.name }
        val [arguments, assignments] = headerPropertiesOfNode(builder).partition {
            it.field.name in constructorFields || it.field.name in config.extraArguments
        }
        for (assignment in assignments) {
            check(assignment.field.isMutable) {
                "${builder.element.name}.${assignment.field.name} is neither a constructor parameter nor mutable"
            }
        }

        val usesFactory = config.creation is Creation.FactoryMethod || config.beforeBuild != null || config.afterBuild != null
        if (!usesFactory) {
            println("@Suppress(\"UnusedReceiverParameter\") // Not created by the factory, but for uniformity `build` is on it.")
        }
        if (builder.isCreatedByEntryPoint) println("@PublishedApi")
        print("internal fun ", irFactoryType.render(), ".build(builder: ", builder.ref.render())
        for ([name, type] in config.extraParameters) {
            print(", ", name, ": ", type.render())
        }
        println("): ", builder.element.render(), " {")
        withIndent {
            config.beforeBuild?.let { println(it, "(builder)") }
            print("val node = ")
            when (val creation = config.creation) {
                is Creation.FactoryMethod -> print(creation.name)
                is Creation.Function -> {
                    addImport(creation.function)
                    print(creation.function.typeName)
                }
            }
            println("(")
            withIndent {
                for (argument in arguments) {
                    println(argument.field.name, " = builder.", argument.field.name, ",")
                }
                val symbol = if (builder.hasPreallocatedSymbol) "builder.symbol" else "${symbolImplType(builder).render()}()"
                println("symbol = ", symbol, ",")
                for ([name, _] in config.extraParameters) {
                    println(name, " = ", name, ",")
                }
            }
            println(")")
            for (assignment in assignments) {
                println("node.", assignment.field.name, " = builder.", assignment.field.name)
            }
            println("builder.parent?.let { node.parent = it }")
            config.afterBuild?.let { println(it, "(builder, node)") }
            println("return node")
        }
        println("}")
    }

    private val DeclarationBuilder.hasPreallocatedSymbol: Boolean
        get() = preallocatesSymbol || ancestors.any { it.preallocatesSymbol }

    private fun symbolType(builder: DeclarationBuilder): TypeRef =
        builder.element.allFields.single { it.name == "symbol" }.typeRef

    private fun symbolImplType(builder: DeclarationBuilder): TypeRef {
        val symbolName = when (val symbolType = symbolType(builder)) {
            is ClassRef<*> -> symbolType.typeName
            is ElementOrRef<*> -> symbolType.element.typeName
            else -> error("Unexpected symbol type of ${builder.element.name}: $symbolType")
        }
        return type(Packages.symbolsImpl, symbolName + "Impl", TypeKind.Class)
    }
}
