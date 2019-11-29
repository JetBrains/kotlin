/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.*
import java.io.File
import java.io.PrintWriter
import java.util.*
import kotlin.math.abs

val COPYRIGHT = """
/*
 * Copyright 2010-${GregorianCalendar()[Calendar.YEAR]} JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
""".trimIndent()

const val BASE_PACKAGE = "org.jetbrains.kotlin.fir"
//const val BASE_PATH = "compiler/fir/tree/src/"
private const val VISITOR_PACKAGE = "org.jetbrains.kotlin.fir.visitors"
private const val INDENT = "    "
val GENERATED_MESSAGE = """
    /*
     * This file was generated automatically
     * DO NOT MODIFY IT MANUALLY
     */
     """.trimIndent()

fun printElements(builder: AbstractFirTreeBuilder, generationPath: File) {
    builder.elements.forEach { it.generateCode(generationPath) }
    builder.elements.flatMap { it.allImplementations }.forEach { it.generateCode(generationPath) }

    printVisitor(builder.elements, generationPath)
    printVisitorVoid(builder.elements, generationPath)
    printTransformer(builder.elements, generationPath)
}

fun PrintWriter.printCopyright() {
    println(COPYRIGHT)
    println()
}

fun PrintWriter.printGeneratedMessage() {
    println(GENERATED_MESSAGE)
    println()
}

fun printVisitor(elements: List<Element>, generationPath: File) {
    val dir = File(generationPath, VISITOR_PACKAGE.replace(".", "/"))
    dir.mkdirs()
    File(dir, "FirVisitor.kt").printWriter().use { printer ->
        with(printer) {
            printCopyright()
            println("package $VISITOR_PACKAGE")
            println()
            elements.forEach { println("import ${it.fullQualifiedName}") }
            println()
            printGeneratedMessage()

            println("abstract class FirVisitor<out R, in D> {")

            indent()
            println("abstract fun visitElement(element: FirElement, data: D): R")
            println()
            for (element in elements) {
                if (element == AbstractFirTreeBuilder.baseFirElement) continue
                with(element) {
                    indent()
                    val varName = safeDecapitalizedName
                    println("open fun ${typeParameters}visit$name($varName: $typeWithArguments, data: D): R${multipleUpperBoundsList()} = visitElement($varName, data)")
                    println()
                }
            }
            println("}")
        }
    }
}

fun printVisitorVoid(elements: List<Element>, generationPath: File) {
    val dir = File(generationPath, VISITOR_PACKAGE.replace(".", "/"))
    dir.mkdirs()
    File(dir, "FirVisitorVoid.kt").printWriter().use { printer ->
        with(printer) {
            printCopyright()
            println("package $VISITOR_PACKAGE")
            println()
            elements.forEach { println("import ${it.fullQualifiedName}") }
            println()
            printGeneratedMessage()

            println("abstract class FirVisitorVoid : FirVisitor<Unit, Nothing?>() {")

            indent()
            println("abstract fun visitElement(element: FirElement)")
            println()
            for (element in elements) {
                if (element == AbstractFirTreeBuilder.baseFirElement) continue
                with(element) {
                    indent()
                    val varName = safeDecapitalizedName
                    println("open fun ${typeParameters}visit$name($varName: $typeWithArguments)${multipleUpperBoundsList()}{")
                    indent(2)
                    println("visitElement($varName)")
                    indent()
                    println("}")
                    println()
                }
            }

            for (element in elements) {
                with(element) {
                    indent()
                    val varName = safeDecapitalizedName
                    println("final override fun ${typeParameters}visit$name($varName: $typeWithArguments, data: Nothing?)${multipleUpperBoundsList()}{")
                    indent(2)
                    println("visit$name($varName)")
                    indent()
                    println("}")
                    println()
                }
            }
            println("}")
        }
    }
}

fun Element.generateCode(generationPath: File) {
    val dir = generationPath.resolve(packageName.replace(".", "/"))
    dir.mkdirs()
    val file = File(dir, "$type.kt")
    file.printWriter().use { printer ->
        with(printer) {
            printCopyright()
            println("package $packageName")
            println()
            val imports = collectImports()
            imports.forEach { println("import $it") }
            if (imports.isNotEmpty()) {
                println()
            }
            printGeneratedMessage()
            printElement(this@generateCode)
        }
    }
}

fun Implementation.generateCode(generationPath: File) {
    val dir = generationPath.resolve(packageName.replace(".", "/"))
    dir.mkdirs()
    val file = File(dir, "$type.kt")
    file.printWriter().use { printer ->
        with(printer) {
            printCopyright()
            println("package $packageName")
            println()
            val imports = collectImports()
            imports.forEach { println("import $it") }
            if (imports.isNotEmpty()) {
                println()
            }
            printGeneratedMessage()
            printImplementation(this@generateCode)
        }
    }
}

val KindOwner.needPureAbstractElement: Boolean get() = (kind != Implementation.Kind.Interface) && !allParents.any { it.kind == Implementation.Kind.AbstractClass }

fun Implementation.collectImports(): List<String> {
    return element.collectImportsInternal(
        listOf(
            element.fullQualifiedName)
                + usedTypes.mapNotNull { it.fullQualifiedName } + parents.mapNotNull { it.fullQualifiedName }
                + listOfNotNull(pureAbstractElementType.fullQualifiedName?.takeIf { needPureAbstractElement }
        ),
        isImpl = true
    )
}

fun Element.collectImports(): List<String> {
    val baseTypes = parents.mapTo(mutableListOf()) { it.fullQualifiedName }
    baseTypes += parentsArguments.values.flatMap { it.values }.mapNotNull { it.fullQualifiedName }
    val isBaseFirElement = this == AbstractFirTreeBuilder.baseFirElement
    if (isBaseFirElement) {
        baseTypes += compositeTransformResultType.fullQualifiedName!!
    }
    if (needPureAbstractElement) {
        baseTypes += pureAbstractElementType.fullQualifiedName!!
    }
    return collectImportsInternal(
        baseTypes,
        isImpl = false
    )
}

fun Element.collectImportsInternal(base: List<String>, isImpl: Boolean): List<String> {
    val fqns = base + allFields.mapNotNull { it.fullQualifiedName } +
            allFields.flatMap { it.arguments.mapNotNull { it.fullQualifiedName } } +
            typeArguments.flatMap { it.upperBounds.mapNotNull { it.fullQualifiedName } }
    val realPackageName = if (isImpl) "$packageName.impl." else "$packageName."
    return fqns.filter { fqn ->
        fqn.dropLastWhile { it != '.' } != realPackageName
    }.distinct().sorted() + "$VISITOR_PACKAGE.*"
}

val Field.isVal: Boolean get() = this is FieldList || (this is FieldWithDefault && origin is FieldList) || !isMutable

fun PrintWriter.printFieldWithDefaultInImplementation(field: Field) {
    val defaultValue = field.defaultValue
    indent()
    print("override ")
    if (field.isLateinit) {
        print("lateinit ")
    }
    if (field.isVal) {
        print("val")
    } else {
        print("var")
    }
    print(" ${field.name}: ${field.mutableType}")
    if (field.isLateinit) {
        println()
        return
    } else {
        print(" ")
    }
    if (field.withGetter) {
        if (field.customSetter != null) {
            println()
            indent(2)
        }
        print("get() ")
    }
    requireNotNull(defaultValue) {
        "No default value for $field"
    }
    println("= $defaultValue")
    field.customSetter?.let {
        indent(2)
        println("set(value) {")
        indent(3)
        println(it)
        indent(2)
        println("}")
    }
}

fun PrintWriter.printImplementation(implementation: Implementation) {
    fun Field.transform() {
        when (this) {
            is FieldWithDefault -> origin.transform()

            is FirField ->
                println("$name = ${name}${call()}transformSingle(transformer, data)")

            is FieldList -> {
                println("${name}.transformInplace(transformer, data)")
            }

            else -> throw IllegalStateException()
        }
    }

    with(implementation) {
        print("${kind!!.title} $type")
        print(element.typeParameters)
        val fieldsWithoutDefault = allFields.filter { it.defaultValue == null && !it.isLateinit }
        val fieldsWithDefault = allFields.filter { it.defaultValue != null || it.isLateinit }

        val isInterface = kind == Implementation.Kind.Interface
        val isAbstract = kind == Implementation.Kind.AbstractClass

        fun abstract() {
            if (isAbstract) {
                print("abstract ")
            }
        }

        if (!isInterface && !isAbstract && fieldsWithoutDefault.isNotEmpty()) {
            println("(")
            fieldsWithoutDefault.forEachIndexed { i, field ->
                val end = if (i == fieldsWithoutDefault.size - 1) "" else ","
                printField(field, isImplementation = true, override = true, end = end, withIndent = true)
            }
            print(")")
        }

        print(" : ")
        if (!isInterface && !allParents.any { it.kind == Implementation.Kind.AbstractClass }) {
            print("${pureAbstractElementType.type}(), ")
        }
//        print(element.typeWithArguments)
        print(allParents.joinToString { "${it.typeWithArguments}${it.kind.braces()}" })
        println(" {")

        if (isInterface || isAbstract) {
            allFields.forEach {
                indent()
                abstract()
                printField(it, isImplementation = true, override = true, end = "", withIndent = false)
            }
        } else {
            fieldsWithDefault.forEach {
                printFieldWithDefaultInImplementation(it)
            }
            if (fieldsWithDefault.isNotEmpty()) {
                println()
            }
        }

        element.allFields.filter { it.type.contains("Symbol") && it !is FieldList }
            .takeIf { it.isNotEmpty() && !isInterface && !isAbstract && !element.type.contains("Reference")}
            ?.let { symbolFields ->
                indent(1)
                println("init {")
                for (symbolField in symbolFields) {
                    indent(2)
                    println("${symbolField.name}${symbolField.call()}bind(this)")
                }
                indent(1)
                println("}")
                println()
            }

        fun Field.acceptString(): String = "${name}${call()}accept(visitor, data)"
        if (!isInterface && !isAbstract) {

            indent(1)
            print("override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {")


            if (element.allFirFields.isNotEmpty()) {
                println()
                for (field in allFields.filter { it.isFirType }) {
                    if (field.withGetter || !field.needAcceptAndTransform) continue
                    when (field.name) {
                        "explicitReceiver" -> {
                            val explicitReceiver = implementation["explicitReceiver"]!!
                            val dispatchReceiver = implementation["dispatchReceiver"]!!
                            val extensionReceiver = implementation["extensionReceiver"]!!
                            println(
                                """
                |        ${explicitReceiver.acceptString()}
                |        if (dispatchReceiver !== explicitReceiver) {
                |            ${dispatchReceiver.acceptString()}
                |        }
                |        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
                |            ${extensionReceiver.acceptString()}
                |        }
                    """.trimMargin()
                            )
                        }

                        "dispatchReceiver", "extensionReceiver", "subjectVariable" -> {
                        }
                        "companionObject" -> {
                        }

                        else -> {
                            if (type in setOf("FirClassImpl", "FirSealedClassImpl") && field.name == "declarations") {
                                indent(2)
                                println("(declarations.firstOrNull { it is FirConstructorImpl } as? FirConstructorImpl)?.typeParameters?.forEach { it.accept(visitor, data) }")
                            }
                            if (type == "FirWhenExpressionImpl" && field.name == "subject") {
                                println(
                                    """
            |        if (subjectVariable != null) {
            |            subjectVariable.accept(visitor, data)
            |        } else {
            |            subject?.accept(visitor, data)
            |        }
                """.trimMargin()
                                )
                            } else {
                                indent(2)
                                when (field.origin) {
                                    is FirField -> {
                                        println(field.acceptString())
                                    }

                                    is FieldList -> {
                                        println("${field.name}.forEach { it.accept(visitor, data) }")
                                    }

                                    else -> throw IllegalStateException()
                                }
                            }
                        }

                    }
                }
                indent()
            }
            println("}")
            println()
        }

        indent()
        abstract()
        print("override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): $typeWithArguments")
        if (!isInterface && !isAbstract) {
            println(" {")
            for (field in allFields) {
                when {
                    !field.isMutable || !field.isFirType || field.withGetter || !field.needAcceptAndTransform -> {
                    }
                    field.name == "explicitReceiver" -> {
                        val explicitReceiver = implementation["explicitReceiver"]!!
                        val dispatchReceiver = implementation["dispatchReceiver"]!!
                        val extensionReceiver = implementation["extensionReceiver"]!!
                        if (explicitReceiver.isMutable) {
                            indent(2)
                            println("explicitReceiver = explicitReceiver${explicitReceiver.call()}transformSingle(transformer, data)")
                        }
                        if (dispatchReceiver.isMutable) {
                            println(
                                """
                            |        if (dispatchReceiver !== explicitReceiver) {
                            |            dispatchReceiver = dispatchReceiver.transformSingle(transformer, data)
                            |        }
                        """.trimMargin()
                            )
                        }
                        if (extensionReceiver.isMutable) {
                            println(
                                """
                            |        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
                            |            extensionReceiver = extensionReceiver.transformSingle(transformer, data)
                            |        }
                        """.trimMargin()
                            )
                        }
                    }
                    field.name in setOf("dispatchReceiver", "extensionReceiver") -> {
                    }
                    type in setOf("FirClassImpl", "FirSealedClassImpl") && field.name == "declarations" -> {
                        indent(2)
                        println("(declarations.firstOrNull { it is FirConstructorImpl } as? FirConstructorImpl)?.typeParameters?.transformInplace(transformer, data)")
                        indent(2)
                        println("declarations.transformInplace(transformer, data)")
                    }
                    field.name == "companionObject" -> {
                        indent(2)
                        println("companionObject = declarations.asSequence().filterIsInstance<FirRegularClass>().firstOrNull { it.status.isCompanion }")
                    }
                    field.needsSeparateTransform -> {
                        indent(2)
                        println("transform${field.name.capitalize()}(transformer, data)")
                    }
                    !element.needTransformOtherChildren -> {
                        indent(2)
                        field.transform()
                    }
                    else -> {
                    }
                }
            }
            if (element.needTransformOtherChildren) {
                indent(2)
                println("transformOtherChildren(transformer, data)")
            }
            indent(2)
            println("return this")
            indent()
            println("}")
        } else {
            println()
        }

        for (field in allFields) {
            if (!field.needsSeparateTransform) continue
            println()
            indent()
            abstract()
            print("override ${field.transformFunctionDeclaration(typeWithArguments)}")
            if (isInterface || isAbstract) {
                println()
                continue
            }
            println(" {")
            if (field.isMutable && field.isFirType) {
                // TODO: replace with smth normal
                if (type == "FirWhenExpressionImpl" && field.name == "subject") {
                    println("""
            |        if (subjectVariable != null) {
            |            subjectVariable = subjectVariable?.transformSingle(transformer, data)
            |        } else {
            |            subject = subject?.transformSingle(transformer, data)
            |        }
                """.trimMargin())
                } else {
                    indent(2)
                    field.transform()
                }
            }
            indent(2)
            println("return this")
            indent()
            println("}")
        }

        if (element.needTransformOtherChildren) {
            println()
            indent()
            abstract()
            print("override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): $typeWithArguments")
            if (isInterface || isAbstract) {
                println()
            } else {
                println(" {")
                for (field in allFields) {
                    if (!field.isMutable || !field.isFirType || field.name == "subjectVariable") continue
                    if (!field.needsSeparateTransform) {
                        indent(2)
                        field.transform()
                    }
                }
                indent(2)
                println("return this")
                indent()
                println("}")
            }
        }

        for (field in allFields.filter { it.withReplace }) {
            println()
            indent()
            abstract()
            print("override ${field.replaceFunctionDeclaration()}")
            if (isInterface || isAbstract) {
                println()
                continue
            }
            print(" {")
            if (!field.isMutable) {
                println("}")
                continue
            }
            println()
            indent(2)
            val newValue = "new${field.name.capitalize()}"
            when {
                field.withGetter -> {
                }

                field.origin is FieldList -> {
                    println("${field.name}.clear()")
                    indent(2)
                    println("${field.name}.addAll($newValue)")
                }

                else -> {
                    println("${field.name} = $newValue")
                }
            }
            indent()
            println("}")
        }

        println("}")
    }
}

fun Field.transformFunctionDeclaration(returnType: String): String {
    return transformFunctionDeclaration(name.capitalize(), returnType)
}

fun transformFunctionDeclaration(transformName: String, returnType: String): String {
    return "fun <D> transform$transformName(transformer: FirTransformer<D>, data: D): $returnType"
}

fun Field.replaceFunctionDeclaration(): String {
    val capName = name.capitalize()
    return "fun replace$capName(new$capName: $typeWithArgumentsWithoutNullablity)"
}

fun printTransformer(elements: List<Element>, generationPath: File) {
    val dir = File(generationPath, VISITOR_PACKAGE.replace(".", "/"))
    dir.mkdirs()
    File(dir, "FirTransformer.kt").printWriter().use { printer ->
        with(printer) {
            printCopyright()
            println("package $VISITOR_PACKAGE")
            println()
            elements.forEach { println("import ${it.fullQualifiedName}") }
            println("import ${compositeTransformResultType.fullQualifiedName}")
            println()
            printGeneratedMessage()

            println("abstract class FirTransformer<in D> : FirVisitor<CompositeTransformResult<FirElement>, D>() {")
            println()
            indent()
            println("abstract fun <E : FirElement> transformElement(element: E, data: D): CompositeTransformResult<E>")
            println()
            for (element in elements) {
                if (element == AbstractFirTreeBuilder.baseFirElement) continue
                indent()
                val varName = element.safeDecapitalizedName
                print("open fun ")
                element.typeParameters.takeIf { it.isNotBlank() }?.let { print(it) }
                println("transform${element.name}($varName: ${element.typeWithArguments}, data: D): CompositeTransformResult<${element.transformerType.typeWithArguments}>${element.multipleUpperBoundsList()}{")
                indent(2)
                println("return transformElement($varName, data)")
                indent()
                println("}")
                println()
            }

            for (element in elements) {
                indent()
                val varName = element.safeDecapitalizedName
                print("final override fun ")
                element.typeParameters.takeIf { it.isNotBlank() }?.let { print(it) }

                println("visit${element.name}($varName: ${element.typeWithArguments}, data: D): CompositeTransformResult<${element.transformerType.typeWithArguments}>${element.multipleUpperBoundsList()}{")
                indent(2)
                println("return transform${element.name}($varName, data)")
                indent()
                println("}")
                println()
            }
            println("}")
        }
    }
}

fun PrintWriter.printField(field: Field, isImplementation: Boolean, override: Boolean, end: String, withIndent: Boolean) {
    if (withIndent) {
        indent()
    }
    if (override) {
        print("override ")
    }
    if (!isImplementation || field.isVal) {
        print("val")
    } else {
        print("var")
    }
    val type = if (isImplementation) field.mutableType else field.typeWithArguments
    println(" ${field.name}: $type$end")
}

val Field.mutableType: String get() = when (this) {
    is FieldList -> if (isMutable) "Mutable$typeWithArguments" else typeWithArguments
    is FieldWithDefault -> if (isMutable) origin.mutableType else typeWithArguments
    else -> typeWithArguments
}

fun Field.call(): String = if (nullable) "?." else "."

fun Element.multipleUpperBoundsList(): String {
    return typeArguments.filterIsInstance<TypeArgumentWithMultipleUpperBounds>().takeIf { it.isNotEmpty() }?.let { arguments ->
        val upperBoundsList = arguments.joinToString(", ", postfix = " ") { argument ->
            argument.upperBounds.joinToString(", ") { upperBound -> "${argument.name} : ${upperBound.typeWithArguments}" }
        }
        " where $upperBoundsList"
    } ?: " "
}

fun Implementation.Kind?.braces(): String = when (this) {
    Implementation.Kind.Interface -> ""
    Implementation.Kind.OpenClass, Implementation.Kind.AbstractClass -> "()"
    else -> throw IllegalStateException(this.toString())
}

fun PrintWriter.printElement(element: Element) {
    with(element) {
        val isInterface = kind == Implementation.Kind.Interface

        fun abstract() {
            indent()
            if (!isInterface) {
                print("abstract ")
            }
        }

        fun override() {
            if (this != AbstractFirTreeBuilder.baseFirElement) {
                print("override ")
            }
        }

        print("${kind!!.title} $type")
        if (typeArguments.isNotEmpty()) {
            print(typeArguments.joinToString(", ", "<", ">") { it.toString() })
        }
        val needPureAbstractElement = !isInterface && !allParents.any { it.kind == Implementation.Kind.AbstractClass }

        if (parents.isNotEmpty() || needPureAbstractElement) {
            print(" : ")
            if (needPureAbstractElement) {
                print("${pureAbstractElementType.type}()")
                if (parents.isNotEmpty()) {
                    print(", ")
                }
            }
            print(parents.joinToString(", ") {
                var result = it.type
                parentsArguments[it]?.let { arguments ->
                    result += arguments.values.joinToString(", ", "<", ">") { it.typeWithArguments }
                }
                result + it.kind.braces()
            })
        }
        print(multipleUpperBoundsList())
        println("{")
        allFields.forEach {
            abstract()
            printField(it, isImplementation = false, override = it.fromParent, end = "", withIndent = false)
        }
        if (allFields.isNotEmpty()) {
            println()
        }

        indent()
        override()
        println("fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visit$name(this, data)")

        fields.filter { it.withReplace }.forEach {
            println()
            abstract()
            if (it.fromParent) print("override ")
            println(it.replaceFunctionDeclaration())
        }

        for (field in allFields) {
            if (!field.needsSeparateTransform) continue
            println()
            abstract()
            if (field.fromParent) {
                print("override ")
            }
            println(field.transformFunctionDeclaration(typeWithArguments))
        }
        if (needTransformOtherChildren) {
            println()
            abstract()
            if (element.parents.any { it.needTransformOtherChildren }) {
                print("override ")
            }
            println(transformFunctionDeclaration("OtherChildren", typeWithArguments))
        }

        if (element == AbstractFirTreeBuilder.baseFirElement) {
            require(isInterface)
            println()
            indent()
            println("fun accept(visitor: FirVisitorVoid) = accept(visitor, null)")
            println()
            indent()
            println("fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D)")
            println()
            indent()
            println("fun acceptChildren(visitor: FirVisitorVoid) = acceptChildren(visitor, null)")
            println()
            indent()
            println("@Suppress(\"UNCHECKED_CAST\")")
            indent()
            println("fun <E : FirElement, D> transform(visitor: FirTransformer<D>, data: D): CompositeTransformResult<E> =")
            indent(2)
            println("accept(visitor, data) as CompositeTransformResult<E>")
            println()
            indent()
            println("fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement")
        }
        println("}")
    }
}

// --------------------------------------- Helpers ---------------------------------------

fun PrintWriter.indent(n: Int = 1) {
    print(INDENT.repeat(n))
}

val Element.safeDecapitalizedName: String get() = if (name == "Class") "klass" else name.decapitalize()

val Importable.typeWithArguments: String
    get() = when (this) {
        is AbstractElement -> type + generics
        is Implementation -> type + element.generics
        is FirField -> element.typeWithArguments + if (nullable) "?" else ""
        is Field -> type + generics + if (nullable) "?" else ""
        is Type -> type + generics
        is ImplementationWithArg -> type + generics
        else -> throw IllegalArgumentException()
    }

val Importable.typeWithArgumentsWithoutNullablity: String get() = typeWithArguments.dropLastWhile { it == '?' }

val ImplementationWithArg.generics: String
    get() = argument?.let { "<${it.type}>" } ?: ""

val AbstractElement.generics: String
    get() = typeArguments.takeIf { it.isNotEmpty() }
        ?.let { it.joinToString(", ", "<", ">") { it.name } }
        ?: ""

val Field.generics: String
    get() = arguments.takeIf { it.isNotEmpty() }
        ?.let { it.joinToString(", ", "<", ">") { it.typeWithArguments } }
        ?: ""

val Element.typeParameters: String
    get() = typeArguments.takeIf { it.isNotEmpty() }
        ?.joinToString(", ", "<", "> ")
        ?: ""

val Type.generics: String
    get() = arguments.takeIf { it.isNotEmpty() }?.joinToString(",", "<", ">") ?: ""