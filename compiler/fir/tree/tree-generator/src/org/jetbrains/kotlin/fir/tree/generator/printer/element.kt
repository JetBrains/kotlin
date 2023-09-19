/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.fir.tree.generator.util.get
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

fun Element.generateCode(generationPath: File): GeneratedFile =
    printGeneratedType(generationPath, TREE_GENERATOR_README, packageName, type) {
        val imports = collectImports()
        imports.forEach { println("import $it") }
        if (imports.isNotEmpty()) {
            println()
        }
        printElement(this@generateCode)
    }

fun SmartPrinter.printElement(element: Element) {
    with(element) {
        val isInterface = kind == ImplementationKind.Interface || kind == ImplementationKind.SealedInterface
        fun abstract() {
            if (!isInterface) {
                print("abstract ")
            }
        }

        fun override() {
            if (!isRootElement) {
                print("override ")
            }
        }

        print("${kind!!.title} $type")
        print(typeParameters())
        val parentRefs = element.parentRefs
        if (parentRefs.isNotEmpty()) {
            print(
                parentRefs.sortedBy { it.typeKind }.joinToString(prefix = " : ") { parent ->
                    parent.typeWithArguments + parent.inheritanceClauseParenthesis()
                }
            )
        }
        print(multipleUpperBoundsList())
        println(" {")
        withIndent {
            allFields.forEach { field ->
                if (field.isFinal && field.fromParent || field.isParameter) return@forEach
                printField(field, isImplementation = false, override = field.fromParent) {
                    if (!field.isFinal) {
                        abstract()
                    }
                }
            }
            if (allFields.isNotEmpty()) {
                println()
            }

            override()
            println("fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =")
            withIndent {
                println("visitor.visit${element.name}(this, data)")
            }

            println()
            println("@Suppress(\"UNCHECKED_CAST\")")
            override()
            println("fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =")
            withIndent {
                println("transformer.transform$name(this, data) as E")
            }

            fun Field.replaceDeclaration(override: Boolean, overridenType: TypeRef? = null, forceNullable: Boolean = false) {
                println()
                if (name == "source") {
                    println("@FirImplementationDetail")
                }
                abstract()
                if (override) print("override ")
                println(replaceFunctionDeclaration(overridenType, forceNullable))
            }

            allFields.filter { it.withReplace }.forEach {
                val override = overridenFields[it, it] &&
                        !(it.name == "source" && fullQualifiedName.endsWith("FirQualifiedAccessExpression"))
                it.replaceDeclaration(override, forceNullable = it.useNullableForReplace)
                for (overridenType in it.overridenTypes) {
                    it.replaceDeclaration(true, overridenType)
                }
            }

            for (field in allFields) {
                if (!field.needsSeparateTransform) continue
                println()
                abstract()
                if (field.fromParent && field.parentHasSeparateTransform) {
                    print("override ")
                }
                println(field.transformFunctionDeclaration(typeWithArguments))
            }
            if (needTransformOtherChildren) {
                println()
                abstract()
                if (element.elementParents.any { it.element.needTransformOtherChildren }) {
                    print("override ")
                }
                println(transformFunctionDeclaration("OtherChildren", typeWithArguments))
            }

            if (element.isRootElement) {
                require(isInterface) {
                    "$element must be an interface"
                }
                println()
                println("fun accept(visitor: FirVisitorVoid) = accept(visitor, null)")
                println()
                println("fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D)")
                println()
                println("fun acceptChildren(visitor: FirVisitorVoid) = acceptChildren(visitor, null)")
                println()
                println("fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement")
            }
        }
        println("}")
    }
}
