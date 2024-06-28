/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator

import org.jetbrains.kotlin.fir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

private typealias Alias = String
private typealias Fqn = String

private const val CHECKERS_COMPONENT_INTERNAL_ANNOTATION = "@CheckersComponentInternal"
private const val CHECKERS_COMPONENT_INTERNAL_FQN = "org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal"
private const val MPP_CHECKER_KIND_FQN = "org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind"
private const val MPP_CHECKER_WITH_KIND_FQN = "org.jetbrains.kotlin.fir.analysis.checkers.FirCheckerWithMppKind"

class Generator(
    private val configuration: CheckersConfiguration,
    generationPath: File,
    private val packageName: String,
    private val abstractCheckerName: String
) {
    private val generationPath: File = getGenerationPath(generationPath, packageName)

    private fun generateAliases() {
        val filename = "${abstractCheckerName}Aliases.kt"
        generationPath.resolve(filename).writeToFileUsingSmartPrinterIfFileContentChanged {
            printPackageAndCopyright()
            printGeneratedMessage()
            configuration.aliases.keys
                .mapNotNull { it.qualifiedName }
                .sorted()
                .forEach { println("import $it") }
            println()
            for ((kClass, alias) in configuration.aliases) {
                val typeParameters =
                    if (kClass.typeParameters.isEmpty()) ""
                    else kClass.typeParameters.joinToString(separator = ",", prefix = "<", postfix = ">") { "*" }
                println("typealias $alias = $abstractCheckerName<${kClass.simpleName}$typeParameters>")
            }
        }
    }

    private fun generateAbstractCheckersComponent() {
        val filename = "${checkersComponentName}.kt"
        generationPath.resolve(filename).writeToFileUsingSmartPrinterIfFileContentChanged {
            printPackageAndCopyright()
            printImports()
            printGeneratedMessage()

            println("abstract class $checkersComponentName {")
            withIndent {
                println("companion object {")
                withIndent {
                    println("val EMPTY: $checkersComponentName = object : $checkersComponentName() {}")
                }
                println("}")
                println()

                for (alias in configuration.aliases.values) {
                    println("open ${alias.valDeclaration} = emptySet()")
                }
                println()

                for ((fieldName, classFqn) in configuration.additionalCheckers) {
                    val fieldClassName = classFqn.simpleName
                    println("open val $fieldName: ${fieldClassName.setType} = emptySet()")
                }
                if (configuration.additionalCheckers.isNotEmpty()) {
                    println()
                }

                for ((kClass, alias) in configuration.aliases) {
                    print("$CHECKERS_COMPONENT_INTERNAL_ANNOTATION internal val ${alias.allFieldName}: ${alias.setType} by lazy { ${alias.fieldName}")
                    for (parent in configuration.parentsMap.getValue(kClass)) {
                        val parentAlias = configuration.aliases.getValue(parent)
                        print(" + ${parentAlias.fieldName}")
                    }
                    println(" }")
                }
            }
            println("}")
        }
    }

    private fun generateComposedComponent() {
        val composedComponentName = "Composed$checkersComponentName"
        val filename = "${composedComponentName}.kt"
        generationPath.resolve(filename).writeToFileUsingSmartPrinterIfFileContentChanged {
            printPackageAndCopyright()
            printImports(MPP_CHECKER_KIND_FQN, MPP_CHECKER_WITH_KIND_FQN)
            printGeneratedMessage()
            println("class $composedComponentName(val predicate: (FirCheckerWithMppKind) -> Boolean) : $checkersComponentName() {")
            withIndent {
                println("constructor(mppKind: MppCheckerKind) : this({ it.mppKind == mppKind })")
                println()

                // public overrides
                for (alias in configuration.aliases.values) {
                    println("override ${alias.valDeclaration}")
                    withIndent {
                        println("get() = _${alias.fieldName}")
                    }
                }
                for ((fieldName, classFqn) in configuration.additionalCheckers) {
                    println("override val $fieldName: ${classFqn.simpleName.setType}")
                    withIndent {
                        println("get() = _$fieldName")
                    }
                }
                println()

                // private mutable delegates
                for (alias in configuration.aliases.values) {
                    println("private val _${alias.fieldName}: ${alias.mutableSetType} = mutableSetOf()")
                }
                for ((fieldName, classFqn) in configuration.additionalCheckers) {
                    println("private val _$fieldName: ${classFqn.simpleName.mutableSetType} = mutableSetOf()")
                }
                println()

                // register function
                println(CHECKERS_COMPONENT_INTERNAL_ANNOTATION)
                println("fun register(checkers: $checkersComponentName) {")
                withIndent {
                    for (alias in configuration.aliases.values) {
                        println("checkers.${alias.fieldName}.filterTo(_${alias.fieldName}, predicate)")
                    }
                    for (fieldName in configuration.additionalCheckers.keys) {
                        println("checkers.$fieldName.filterTo(_$fieldName, predicate)")
                    }
                }
                println("}")
            }
            println("}")
        }
    }

    private fun SmartPrinter.printPackageAndCopyright() {
        printCopyright()
        println("package $packageName")
        println()
    }

    private fun SmartPrinter.printImports(vararg additionalImports: String) {
        val imports = buildList {
            addAll(configuration.additionalCheckers.values)
            add(CHECKERS_COMPONENT_INTERNAL_FQN)
            addAll(additionalImports)
        }.sorted()

        for (fqn in imports) {
            println("import $fqn")
        }
        println()
    }

    private val Alias.valDeclaration: String
        get() = "val $fieldName: $setType"

    private val Alias.fieldName: String
        get() = removePrefix("Fir").replaceFirstChar(Char::lowercaseChar) + "s"

    private val Alias.allFieldName: String
        get() = "all${fieldName.replaceFirstChar(Char::uppercaseChar)}"

    private val Alias.setType: String
        get() = "Set<$this>"

    private val Alias.mutableSetType: String
        get() = "MutableSet<$this>"

    private val Fqn.simpleName: String
        get() = this.split(".").last()

    private val checkersComponentName = abstractCheckerName.removePrefix("Fir") + "s"

    fun generate() {
        generateAliases()
        generateAbstractCheckersComponent()
        generateComposedComponent()
    }
}
