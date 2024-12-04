/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import org.jetbrains.kotlin.generators.util.getGenerationPath
import org.jetbrains.kotlin.generators.util.printCopyright
import org.jetbrains.kotlin.generators.util.printGeneratedMessage
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File
import kotlin.reflect.KClass
import kotlin.text.removeSuffix

internal typealias Alias = String
private typealias Fqn = String
private typealias Checker = Map.Entry<KClass<*>, Pair<String, Boolean>>

private const val CHECKERS_COMPONENT_INTERNAL = "CheckersComponentInternal"
private const val CHECKERS_COMPONENT_INTERNAL_ANNOTATION = "@$CHECKERS_COMPONENT_INTERNAL"
private const val CHECKERS_COMPONENT_INTERNAL_FQN = "org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal"
private const val MPP_CHECKER_KIND_FQN = "org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind"
private const val MPP_CHECKER_WITH_KIND_FQN = "org.jetbrains.kotlin.fir.analysis.checkers.FirCheckerWithMppKind"

// DiagnosticComponent
private const val FIR_SESSION_FQN = "org.jetbrains.kotlin.fir.FirSession"
private const val DIAGNOSTIC_REPORTER_FQN = "org.jetbrains.kotlin.diagnostics.DiagnosticReporter"
private const val ABSTRACT_DIAGNOSTIC_REPORTER_FQN =
    "org.jetbrains.kotlin.fir.analysis.collectors.components.AbstractDiagnosticCollectorComponent"
private const val CHECKER_CONTEXT_FQN = "org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext"
private const val FIR_FQN = "org.jetbrains.kotlin.fir"
private const val CHECKERS_COMPONENT_FQN = "org.jetbrains.kotlin.fir.analysis.checkersComponent"
private const val FIR_ELEMENT_FQN = "org.jetbrains.kotlin.fir.FirElement"
private const val WITH_ENTRY_FQN = "org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry"
private const val RETHROW_FQN = "org.jetbrains.kotlin.utils.exceptions.rethrowExceptionWithDetails"

class Generator(
    private val configuration: CheckersConfiguration,
    generationPath: File,
    private val packageName: String,
    private val abstractCheckerName: String,
    private val checkMethodTypeParameterConstraint: KClass<out FirElement>,
    private val checkType: KClass<out FirElement>,
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
                println("typealias ${alias.component1()} = $abstractCheckerName<${kClass.simpleName}$typeParameters>")
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

                for ((alias, _) in configuration.aliases.values) {
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
                    print("$CHECKERS_COMPONENT_INTERNAL_ANNOTATION internal val ${alias.component1().allFieldName}: ${alias.component1().arrayType} by lazy { ")
                    val parents = configuration.parentsMap.getValue(kClass)
                    if (parents.isNotEmpty()) {
                        print('(')
                    }
                    print(alias.component1().fieldName)
                    for (parent in parents) {
                        val parentAlias = configuration.aliases.getValue(parent)
                        print(" + ${parentAlias.component1().fieldName}")
                    }
                    if (parents.isNotEmpty()) {
                        print(')')
                    }
                    println(".toTypedArray() }")
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
            printImports(true, MPP_CHECKER_KIND_FQN, MPP_CHECKER_WITH_KIND_FQN)
            printGeneratedMessage()
            println("class $composedComponentName(val predicate: (FirCheckerWithMppKind) -> Boolean) : $checkersComponentName() {")
            withIndent {
                println("constructor(mppKind: MppCheckerKind) : this({ it.mppKind == mppKind })")
                println()

                // public overrides
                for ((alias, _) in configuration.aliases.values) {
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
                for ((alias, _) in configuration.aliases.values) {
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
                    for ((alias, _) in configuration.aliases.values) {
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

    private fun generateDiagnosticComponent() {
        val diagnosticComponentName = "${checkersComponentName}DiagnosticComponent"
        val filename = "$diagnosticComponentName.kt"
        generationPath.resolve(filename).writeToFileUsingSmartPrinterIfFileContentChanged {
            printPackageAndCopyright()
            printImports(
                false,
                FIR_SESSION_FQN,
                DIAGNOSTIC_REPORTER_FQN,
                ABSTRACT_DIAGNOSTIC_REPORTER_FQN,
                MPP_CHECKER_KIND_FQN,
                CHECKER_CONTEXT_FQN,
                "$FIR_FQN.$checkersPackageName.*",
                CHECKERS_COMPONENT_FQN,
                FIR_ELEMENT_FQN,
                WITH_ENTRY_FQN,
                RETHROW_FQN
            )
            printGeneratedMessage()
            println("@OptIn($CHECKERS_COMPONENT_INTERNAL::class)")
            println("class $diagnosticComponentName(")
            withIndent {
                println("session: FirSession,")
                println("reporter: DiagnosticReporter,")
                println("private val checkers: $checkersComponentName,")
            }
            println(") : AbstractDiagnosticCollectorComponent(session, reporter) {")

            withIndent {
                printDiagnosticComponentConstructor()
                println()
                printDiagnosticComponentVisitElementMethod()
                println()
                for ((checker, value) in configuration.aliases) {
                    if (value.component2()) {
                        printDiagnosticComponentVisitMethod(checker, value.component1())
                        println()
                    }
                }

                for ((checker, value) in configuration.visitAlso) {
                    printDiagnosticComponentVisitMethod(checker, value)
                    println()
                }

                printDiagnosticComponentCheckMethod()
            }

            println("}")
        }
    }

    private fun SmartPrinter.printPackageAndCopyright() {
        printCopyright()
        println("package $packageName")
        println()
    }

    private fun SmartPrinter.printImports(includeAdditionalCheckers: Boolean = true, vararg additionalImports: String) {
        val imports = buildList {
            if (includeAdditionalCheckers) {
                addAll(configuration.additionalCheckers.values)
            }
            add(CHECKERS_COMPONENT_INTERNAL_FQN)
            addAll(additionalImports)
        }.sorted()

        for (fqn in imports) {
            println("import $fqn")
        }
        println()
    }

    private fun SmartPrinter.printDiagnosticComponentVisitMethod(checker: KClass<*>, alias: Alias) {
        val elementParamName = if (checker.elementParamName == "class") "klass" else checker.elementParamName

        println("override fun visit${checker.elementName}($elementParamName: ${checker.elementTypeName}, data: CheckerContext) {")
        withIndent {
            println("checkers.${alias.allFieldName}.check($elementParamName, data)")
        }
        println("}")
    }

    private fun SmartPrinter.printDiagnosticComponentConstructor() {
        println("constructor(session: FirSession, reporter: DiagnosticReporter, mppKind: MppCheckerKind) : this(")
        withIndent {
            println("session,")
            println("reporter,")
            println("when (mppKind) {")
            withIndent {
                println("MppCheckerKind.Common -> session.checkersComponent.common$checkersComponentName")
                println("MppCheckerKind.Platform -> session.checkersComponent.platform$checkersComponentName")
            }
            println("}")
        }
        println(")")
    }

    private fun SmartPrinter.printDiagnosticComponentVisitElementMethod() {
        println("override fun visitElement(element: FirElement, data: CheckerContext) {")
        withIndent {
            println("if (element is ${checkType.simpleName}) {")
            withIndent {
                println("error(\"\${element::class.simpleName} should call parent checkers inside \${this::class.simpleName}\")")
            }
            println("}")
        }
        println("}")
    }

    private fun SmartPrinter.printDiagnosticComponentCheckMethod() {
        println("private inline fun <reified E : ${checkMethodTypeParameterConstraint.simpleName}> Array<$abstractCheckerName<E>>.check(")
        withIndent {
            println("element: E,")
            println("context: CheckerContext")
        }
        println(") {")
        withIndent {
            println("for (checker in this) {")
            withIndent {
                println("try {")
                withIndent {
                    println("checker.check(element, context, reporter)")
                }
                println("} catch (e: Exception) {")
                withIndent {
                    println("rethrowExceptionWithDetails(\"Exception in $checkersTypeInErrorMsg checkers\", e) {")
                    withIndent {
                        println("withFirEntry(\"element\", element)")
                        println("context.containingFilePath?.let { withEntry(\"file\", it) }")
                    }
                    println("}")
                }
                println("}")
            }
            println("}")
        }
        println("}")
    }

    private val KClass<*>.elementTypeName: String
        get() = simpleName!!

    private val KClass<*>.elementParamName: String
        get() = elementName.replaceFirstChar(Char::lowercaseChar)

    private val KClass<*>.elementName: String
        get() = elementTypeName.removePrefix("Fir")

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

    private val Alias.arrayType: String
        get() = "Array<$this>"

    private val Fqn.simpleName: String
        get() = this.split(".").last()

    private val checkersComponentName = abstractCheckerName.removePrefix("Fir") + "s"

    private val checkersPackageName = checkersComponentName.removeSuffix("Checkers").lowercase() + "s"

    private val checkersTypeInErrorMsg = abstractCheckerName.removePrefix("Fir").removeSuffix("Checker").lowercase()

    fun generate() {
        generateAliases()
        generateAbstractCheckersComponent()
        generateComposedComponent()
        generateDiagnosticComponent()
    }
}
