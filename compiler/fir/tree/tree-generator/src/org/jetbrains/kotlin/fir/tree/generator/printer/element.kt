/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.*
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.fir.tree.generator.util.get
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

fun Element.generateCode(generationPath: File): GeneratedFile =
    printGeneratedType(generationPath, TREE_GENERATOR_README, packageName, this.typeName) {
        println()
        printElement(this@generateCode)
    }

context(ImportCollector)
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

        printKDoc(element.extendedKDoc())
        print("${kind!!.title} $typeName")
        print(typeParameters())
        val parentRefs = element.parentRefs
        if (parentRefs.isNotEmpty()) {
            print(
                parentRefs.sortedBy { it.typeKind }.joinToString(prefix = " : ") { parent ->
                    parent.render() + parent.inheritanceClauseParenthesis()
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

            if (hasAcceptMethod) {
                if (allFields.isNotEmpty()) {
                    println()
                }

                override()
                println("fun <R, D> accept(visitor: ${firVisitorType.render()}<R, D>, data: D): R =")
                withIndent {
                    println("visitor.visit${element.name}(this, data)")
                }
            }

            if (hasTransformMethod) {
                println()
                println("@Suppress(\"UNCHECKED_CAST\")")
                override()
                println(
                    "fun <E : ",
                    AbstractFirTreeBuilder.baseFirElement.render(),
                    ", D> transform(transformer: ",
                    firTransformerType.render(),
                    "<D>, data: D): E ="
                )
                withIndent {
                    println("transformer.transform$name(this, data) as E")
                }
            }

            fun Field.replaceDeclaration(override: Boolean, overridenType: TypeRefWithNullability? = null, forceNullable: Boolean = false) {
                println()
                if (name == "source") {
                    println("@${firImplementationDetailType.render()}")
                }
                abstract()
                if (override) print("override ")
                println(replaceFunctionDeclaration(overridenType, forceNullable))
            }

            allFields.filter { it.withReplace }.forEach {
                val override = overridenFields[it, it] &&
                        !(it.name == "source" && element == FirTreeBuilder.qualifiedAccessExpression)
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
                println(field.transformFunctionDeclaration(element))
            }
            if (needTransformOtherChildren) {
                println()
                abstract()
                if (element.elementParents.any { it.element.needTransformOtherChildren }) {
                    print("override ")
                }
                println(transformFunctionDeclaration("OtherChildren", element))
            }

            if (element.isRootElement) {
                require(isInterface) {
                    "$element must be an interface"
                }
                println()
                println("fun accept(visitor: ${firVisitorVoidType.render()}) = accept(visitor, null)")
                if (element.hasAcceptChildrenMethod) {
                    println()
                    println("fun <R, D> acceptChildren(visitor: ${firVisitorType.render()}<R, D>, data: D)")
                }
                println()
                println("fun acceptChildren(visitor: ${firVisitorVoidType.render()}) = acceptChildren(visitor, null)")
                if (element.hasTransformChildrenMethod) {
                    println()
                    println("fun <D> transformChildren(transformer: ${firTransformerType.render()}<D>, data: D): FirElement")
                }
            }
        }
        println("}")
    }
}
