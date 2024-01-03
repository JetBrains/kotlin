/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.nio.file.Path
import kotlin.io.path.forEachLine
import kotlin.io.path.notExists

private const val INHERITORS_EXTENSION = "inheritors.txt"
private const val PAIRS_DELIMITER = ", "
private const val VALUE_DELIMITER = ": "

open class AbstractSymbolLightClassesStructureTestBase(
    configurator: AnalysisApiTestConfigurator,
    protected val testPrefix: String,
    override val isTestAgainstCompiledCode: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {
    override val currentExtension: String get() = throw UnsupportedOperationException()
    protected fun doTestInheritors(ktFiles: List<KtFile>, testServices: TestServices) {
        val testData = getTestDataFileSiblingPath(extension = INHERITORS_EXTENSION, testPrefix = testPrefix)
        if (testData.notExists()) return
        val project = ktFiles.first().project

        val queries = parseInheritorsFile(testData)
        val result = buildString {
            for (query in queries) {
                append("subClass")
                append(VALUE_DELIMITER)
                append(query.fqNameToCheck)
                append(PAIRS_DELIMITER)

                append("superClass")
                append(VALUE_DELIMITER)
                append(query.baseFqName)
                append(PAIRS_DELIMITER)

                append("deepSearch")
                append(VALUE_DELIMITER)
                append(query.deep.toString())

                append(" -> ")
                appendLine(query.isInheritor(project).toString())
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(
            actual = result,
            testPrefix = testPrefix,
            extension = INHERITORS_EXTENSION,
        )
    }

    private fun InheritorStructure.isInheritor(project: Project): Boolean {
        val lightClass = findLightClass(fqNameToCheck, project) ?: error("Can't find light class by '$fqNameToCheck' qualifier")
        val baseClass = JavaPsiFacade.getInstance(project).findClass(baseFqName, GlobalSearchScope.allScope(project))
            ?: error("Can't find class by '$baseFqName' qualifier")

        return lightClass.isInheritor(/* baseClass = */ baseClass, /* checkDeep = */ deep)
    }

    private fun parseInheritorsFile(path: Path): Collection<InheritorStructure> = buildList {
        path.forEachLine { line: String ->
            if (line.isBlank()) return@forEachLine

            val arguments = line.split(PAIRS_DELIMITER)
            val fqNameToCheck = arguments.getOrNull(0)
                ?.substringAfter(VALUE_DELIMITER)
                ?: wrongInheritorStructure(line)

            val baseFqName = arguments.getOrNull(1)
                ?.substringAfter(VALUE_DELIMITER)
                ?: wrongInheritorStructure(line)

            val deep = arguments.getOrNull(2)
                ?.substringAfter(VALUE_DELIMITER)
                ?.substringBefore(' ')
                ?.toBoolean()
                ?: wrongInheritorStructure(line)

            add(InheritorStructure(fqNameToCheck = fqNameToCheck, baseFqName = baseFqName, deep = deep))
        }
    }.toSet()

    private fun wrongInheritorStructure(line: String): Nothing = error("Can't parse '$line' line correctly")

    protected fun PrettyPrinter.handleFile(ktFile: KtFile) {
        val text = ktFile.text
        if (ktFile.isCompiled) {
            // A compiled file for a class should only contain a single class declaration. `*Kt.class` files on the other hand may contain
            // top-level callables and need to be skipped.
            val classOrObject = ktFile.declarations.singleOrNull() as? KtClassOrObject ?: return
            handleCompiledClassDeclaration(classOrObject, text)
        } else {
            ktFile.collectDescendantsOfType<KtClassOrObject>()
                .sortedBy { it.fqName?.asString() ?: it.name.toString() }
                .forEach { classOrObject ->
                    handleClassDeclaration(classOrObject, text)
                    appendLine()
                }
        }
    }

    /**
     * [handleCompiledClassDeclaration] uses a custom traversal instead of [forEachDescendantOfType] because trying to access the PSI of
     * compiled code in this test results in exceptions. Hence, we have to traverse nested classes and enum entries manually.
     */
    private fun PrettyPrinter.handleCompiledClassDeclaration(classOrObject: KtClassOrObject, text: String) {
        handleClassDeclaration(classOrObject, text)
        appendLine()

        classOrObject.declarations.forEach { declaration ->
            when (declaration) {
                is KtEnumEntry -> {
                    // We don't call `handleCompiledClassDeclaration` to avoid printing class declarations inside enum entry initializers.
                    handleClassDeclaration(declaration, text)
                    appendLine()
                }
                is KtClassOrObject -> handleCompiledClassDeclaration(declaration, text)
            }
        }
    }

    private fun PrettyPrinter.handleClassDeclaration(declaration: KtClassOrObject, fileText: String) {
        appendLine("${declaration::class.simpleName}:")
        withIndent {
            val lineNumber = fileText.subSequence(0, declaration.startOffset).count { it == '\n' } + 1
            appendLine("line: $lineNumber")
            appendLine("name: ${declaration.name}")
            appendLine("qualifier: ${declaration.fqName}")
            append("light: ")
            val lightClass = declaration.toLightClass()
            if (lightClass != null) {
                handleClass(lightClass)
            } else {
                appendLine("null")
            }
        }
    }

    protected fun PrettyPrinter.handleClass(psiClass: PsiClass) {
        appendLine(psiClass::class.simpleName)
        withIndent {
            appendLine("name: ${psiClass.name}")
            appendLine("qualifier: ${psiClass.qualifiedName}")
            appendCollection("superTypes", psiClass.superTypes.asList()) { it.toString() }
            appendLine("superClass: ${psiClass.superClass?.render()}")
            appendCollection("interfaces", psiClass.interfaces.asList()) { it.render() }
            appendCollection("supers", psiClass.supers.asList()) { it.render() }
        }
    }

    private fun PsiClass.render(): String = "${this::class.simpleName}: $name ($qualifiedName)"
    private inline fun <T> PrettyPrinter.appendCollection(
        name: String,
        collection: Collection<T>,
        crossinline renderer: (T) -> CharSequence,
    ) {
        append("$name: ")
        if (collection.isEmpty()) {
            appendLine("[]")
            return
        }

        withIndentInSquareBrackets {
            printCollection(collection, separator = "\n") {
                append(renderer(it))
            }
        }

        appendLine()
    }

    override fun getRenderResult(ktFile: KtFile, ktFiles: List<KtFile>, testDataFile: Path, module: TestModule, project: Project): String {
        throw UnsupportedOperationException()
    }
}

private data class InheritorStructure(val fqNameToCheck: String, val baseFqName: String, val deep: Boolean)