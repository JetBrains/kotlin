/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation.Kind
import org.jetbrains.kotlin.fir.tree.generator.pureAbstractElementType
import org.jetbrains.kotlin.util.SmartPrinter
import org.jetbrains.kotlin.util.withIndent

import java.io.File

fun Implementation.generateCode(generationPath: File): GeneratedFile {
    val dir = generationPath.resolve(packageName.replace(".", "/"))
    val file = File(dir, "$type.kt")
    val stringBuilder = StringBuilder()
    SmartPrinter(stringBuilder).apply {
        printCopyright()
        println("@file:Suppress(\"DuplicatedCode\")")
        println()
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
    return GeneratedFile(file, stringBuilder.toString())
}

fun SmartPrinter.printImplementation(implementation: Implementation) {
    fun Field.transform() {
        when (this) {
            is FieldWithDefault -> origin.transform()

            is FirField ->
                println("$name = ${name}${call()}transform(transformer, data)")

            is FieldList -> {
                println("${name}.transformInplace(transformer, data)")
            }

            else -> throw IllegalStateException()
        }
    }

    with(implementation) {
        if (requiresOptIn) {
            println("@OptIn(FirImplementationDetail::class)")
        }
        if (!isPublic) {
            print("internal ")
        }
        print("${kind!!.title} $type")
        print(element.typeParameters)

        val isInterface = kind == Kind.Interface || kind == Kind.SealedInterface
        val isAbstract = kind == Kind.AbstractClass || kind == Kind.SealedClass

        fun abstract() {
            if (isAbstract) {
                print("abstract ")
            }
        }

        if (!isInterface && !isAbstract && fieldsWithoutDefault.isNotEmpty()) {
            if (isPublic) {
                print(" @FirImplementationDetail constructor")
            }
            println("(")
            withIndent {

                fieldsWithoutDefault.forEachIndexed { _, field ->
                    printField(field, isImplementation = true, override = true, end = ",")
                }
            }
            print(")")
        }

        print(" : ")
        if (!isInterface && !allParents.any { it.kind == Kind.AbstractClass || it.kind == Kind.SealedClass }) {
            print("${pureAbstractElementType.type}(), ")
        }
        print(allParents.joinToString { "${it.typeWithArguments}${it.kind.braces()}" })
        println(" {")
        withIndent {
            if (isInterface || isAbstract) {
                allFields.forEach {

                    abstract()
                    printField(it, isImplementation = true, override = true, end = "")
                }
            } else {
                fieldsWithDefault.forEach {
                    printFieldWithDefaultInImplementation(it)
                }
                if (fieldsWithDefault.isNotEmpty()) {
                    println()
                }
            }


            element.allFields.filter {
                it.withBindThis && it.type.contains("Symbol") && it !is FieldList && it.name != "companionObjectSymbol"
            }.takeIf {
                it.isNotEmpty() && !isInterface && !isAbstract &&
                        !element.type.contains("Reference")
                        && !element.type.contains("ResolvedQualifier")
                        && !element.type.endsWith("Ref")
            }?.let { symbolFields ->
                println("init {")
                for (symbolField in symbolFields) {
                    withIndent {
                        println("${symbolField.name}${symbolField.call()}bind(this)")
                    }
                }
                println("}")
                println()
            }


            if (!isInterface && !isAbstract) {
                println("override val elementKind get() = FirElementKind.${this.element.name}")
            }

            fun generateReplace(
                field: Field,
                overridenType: Importable? = null,
                forceNullable: Boolean = false,
                body: () -> Unit,
            ) {
                println()
                if (field.name == "source") {
                    println("@FirImplementationDetail")
                }
                abstract()
                print("override ${field.replaceFunctionDeclaration(overridenType, forceNullable)}")
                if (isInterface || isAbstract) {
                    println()
                    return
                }
                print(" {")
                if (!field.isMutable) {
                    println("}")
                    return
                }
                println()
                withIndent {
                    body()
                }
                println("}")
            }

            for (field in allFields.filter { it.withReplace || it.isFirType }) {
                val capitalizedFieldName = field.name.replaceFirstChar(Char::uppercaseChar)
                val newValue = "new$capitalizedFieldName"
                generateReplace(field, forceNullable = field.useNullableForReplace) {
                    when {
                        field.withGetter -> {}

                        field.origin is FieldList -> {
                            println("${field.name}.clear()")
                            println("${field.name}.addAll($newValue)")
                        }

                        else -> {
                            if (field.useNullableForReplace) {
                                println("require($newValue != null)")
                            }
                            println("${field.name} = $newValue")
                        }
                    }
                }

                for (overridenType in field.overridenTypes) {
                    generateReplace(field, overridenType) {
                        println("require($newValue is ${field.typeWithArguments})")
                        println("replace$capitalizedFieldName($newValue)")
                    }
                }
            }
        }
        println("}")
    }
}
