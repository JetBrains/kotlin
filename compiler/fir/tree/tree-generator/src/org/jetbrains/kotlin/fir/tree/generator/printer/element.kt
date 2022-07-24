/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation.Kind
import org.jetbrains.kotlin.fir.tree.generator.pureAbstractElementType
import org.jetbrains.kotlin.fir.tree.generator.util.get
import org.jetbrains.kotlin.util.SmartPrinter
import org.jetbrains.kotlin.util.withIndent

import java.io.File

fun Element.generateCode(generationPath: File): GeneratedFile {
    val dir = generationPath.resolve(packageName.replace(".", "/"))
    val file = File(dir, "$type.kt")
    val stringBuilder = StringBuilder()
    SmartPrinter(stringBuilder).apply {
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
    return GeneratedFile(file, stringBuilder.toString())
}

fun SmartPrinter.printElement(element: Element) {
    with(element) {
        val isInterface = kind == Kind.Interface || kind == Kind.SealedInterface
        fun abstract() {
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
        val needPureAbstractElement = !isInterface && !allParents.any { it.kind == Kind.AbstractClass || it.kind == Kind.SealedClass }

        if (parents.isNotEmpty() || needPureAbstractElement) {
            print(" : ")
            if (needPureAbstractElement) {
                print("${pureAbstractElementType.type}()")
                if (parents.isNotEmpty()) {
                    print(", ")
                }
            }
            print(
                parents.joinToString(", ") {
                    var result = it.type
                    parentsArguments[it]?.let { arguments ->
                        result += arguments.values.joinToString(", ", "<", ">") { it.typeWithArguments }
                    }
                    result + it.kind.braces()
                },
            )
        }
        print(multipleUpperBoundsList())
        println("{")
        withIndent {
            allFields.forEach {
                abstract()
                printField(it, isImplementation = false, override = it.fromParent, end = "")
            }
            if (allFields.isNotEmpty()) {
                println()
            }

            fun Field.replaceDeclaration(override: Boolean, overridenType: Importable? = null, forceNullable: Boolean = false) {
                println()
                if (name == "source") {
                    println("@FirImplementationDetail")
                }
                abstract()
                if (override) print("override ")
                println(replaceFunctionDeclaration(overridenType, forceNullable))
            }

            allFields.filter { it.withReplace || it.isFirType }.forEach {
                val override = overridenFields[it, it] && !(it.name == "source" && fullQualifiedName.endsWith("FirQualifiedAccess"))
                it.replaceDeclaration(override, forceNullable = it.useNullableForReplace)
                for (overridenType in it.overridenTypes) {
                    it.replaceDeclaration(true, overridenType)
                }
            }

            if (element == AbstractFirTreeBuilder.baseFirElement) {
                require(isInterface)
                println()
                println("val elementKind: FirElementKind")
            }
        }
        println("}")

        for (field in allFirFields) {
            if (field.withGetter || (field is FirField && field.nonTraversable)) continue



            println()
            print("inline ")

            val transformName = field.name.replaceFirstChar(Char::uppercaseChar)
            val returnType = typeWithArguments
            val transformTypeParameters = (listOf("D") + typeArguments.map { it.toString() }).joinToString(", ", "<", ">")
            println("fun $transformTypeParameters $returnType.transform$transformName(transformer: FirTransformer<D>, data: D): $returnType${multipleUpperBoundsList()}")
            withIndent {
                // TODO WTF????!?!?!?!?!?
                if (field.name == "subject" && element.type == "FirWhenExpression") {
                    println("""
                    |= apply { if (subjectVariable != null) {
                    |        replaceSubjectVariable(subjectVariable?.transform(transformer, data))
                    |        replaceSubject(subjectVariable?.initializer)
                    |       } else {
                    |           replaceSubject(subject?.transform(transformer, data))
                    |       }
                    |       }
                    """.trimMargin())
                } else {
                    println(" = apply { replace${field.name.replaceFirstChar(Char::uppercaseChar)}(${field.name}${field.call()}transform(transformer, data)) }")
                }
            }
        }
    }
}
