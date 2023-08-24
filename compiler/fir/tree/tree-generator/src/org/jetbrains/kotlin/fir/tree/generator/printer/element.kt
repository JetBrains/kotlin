/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation.Kind
import org.jetbrains.kotlin.fir.tree.generator.model.Importable
import org.jetbrains.kotlin.fir.tree.generator.pureAbstractElementType
import org.jetbrains.kotlin.fir.tree.generator.util.get
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
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
            allFields.forEach { field ->
                if (field.isFinal && field.fromParent || field.isParameter) return@forEach
                if (!field.isFinal) {
                    abstract()
                }
                printField(field, isImplementation = false, override = field.fromParent, end = "")
            }
            if (allFields.isNotEmpty()) {
                println()
            }

            override()
            println("fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visit$name(this, data)")

            println()
            println("@Suppress(\"UNCHECKED_CAST\")")
            override()
            println("fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =")
            withIndent {
                println("transformer.transform$name(this, data) as E")
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
                println("fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement")
            }

            if (element == FirTreeBuilder.expression) {
                println()
                println("/** DO NOT USE! Only for temporary compatibility with Compose. Will be removed soon. KT-61312*/")
                println("val typeRef: org.jetbrains.kotlin.fir.types.FirTypeRef get() = coneTypeOrNull?.let { org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef { type = it } } ?: org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource")
            }
        }

        println("}")
    }
}
