/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.fir.tree.generator.pureAbstractElementType
import org.jetbrains.kotlin.fir.tree.generator.util.get

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
        val isInterface = kind == Implementation.Kind.Interface

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
        val needPureAbstractElement = !isInterface && !allParents.any { it.kind == Implementation.Kind.AbstractClass }

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

            override()
            println("fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visit$name(this, data)")

            fun Field.replaceDeclaration(override: Boolean, overridenType: Importable? = null, forceNullable: Boolean = false) {
                println()
                abstract()
                if (override) print("override ")
                println(replaceFunctionDeclaration(overridenType, forceNullable))
            }

            allFields.filter { it.withReplace }.forEach {
                it.replaceDeclaration(overridenFields[it, it], forceNullable = it.useNullableForReplace)
                for (overridenType in it.overridenTypes) {
                    it.replaceDeclaration(true, overridenType)
                }
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
                println("fun accept(visitor: FirVisitorVoid) = accept(visitor, null)")
                println()
                println("fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D)")
                println()
                println("fun acceptChildren(visitor: FirVisitorVoid) = acceptChildren(visitor, null)")
                println()
                println("@Suppress(\"UNCHECKED_CAST\")")
                println("fun <E : FirElement, D> transform(visitor: FirTransformer<D>, data: D): CompositeTransformResult<E> =")
                withIndent {
                    println("accept(visitor, data) as CompositeTransformResult<E>")
                }
                println()
                println("fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement")
            }
        }
        println("}")
    }
}
