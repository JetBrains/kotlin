/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.jvm.repl

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import java.io.File
import java.util.jar.JarFile

class LibraryDocInfoProvider(val project: Project) {
    private companion object {
        private val LINE_SEPARATOR = System.getProperty("line.separator")

        private val FILTERED_JAVA_TAGS = listOf("see", "author", "since")
    }

    private val kotlinLibraryCache = KotlinLibraryNameCache(project)
    private val javaFileFactory = PsiFileFactory.getInstance(project)

    fun getDoc(language: Language, libraries: List<File>, packageName: String, declarationName: String): String? {
        assert(language == KotlinLanguage.INSTANCE || language == JavaLanguage.INSTANCE)

        for (library in libraries) {
            // Checks also for .java source files
            if (!kotlinLibraryCache.hasSources(library)) {
                continue
            }

            if (language == KotlinLanguage.INSTANCE) {
                getKotlinDoc(library, packageName, declarationName)?.let { return it }
            }

            if (language == JavaLanguage.INSTANCE) {
                getJavaDoc(library, packageName, declarationName)?.let { return it }
            }
        }

        return null
    }

    private fun getKotlinDoc(library: File, packageName: String, declarationName: String): String? {
        assert(declarationName.isNotEmpty())

        val entryName = kotlinLibraryCache.getEntryName(library, packageName, declarationName) ?: return null
        JarFile(library).use { jar ->
            assert(entryName.toLowerCase().endsWith(".kt"))

            val entry = jar.getJarEntry(entryName) ?: return null
            val ktFile = kotlinLibraryCache.ktFactory.createFile(jar.getInputStream(entry).reader().readText())

            fun findDeclaration(current: KtElement, path: List<String>): KtDeclaration? {
                if (path.isEmpty() && current is KtNamedDeclaration) {
                    return current
                }

                if (current is KtDeclarationContainer) {
                    val targetName = path.first()
                    for (declaration in current.declarations) {
                        if (declaration is KtNamedDeclaration && declaration.name == targetName) {
                            return findDeclaration(declaration, path.drop(1))
                        }
                    }
                }

                return null
            }

            val kdoc = findDeclaration(ktFile, declarationName.split('.'))?.docComment ?: return null
            return kdoc.getChildrenOfType<KDocSection>().firstOrNull()?.text?.let { prettifyKdoc(it) }
        }
    }

    private fun getJavaDoc(library: File, packageName: String, declarationName: String): String? {
        JarFile(library).use { jar ->
            val segments = declarationName.split('.')

            // If the file has more than one top-level class, only the public one will be found
            val entryName = packageName.replace('.', '/') + (if (packageName.isEmpty()) "" else "/") + segments.first() + ".java"
            val entry = jar.getJarEntry(entryName) ?: return null
            val fileText = jar.getInputStream(entry).reader().readText()

            val javaFile = javaFileFactory.createFileFromText(entry.name, JavaLanguage.INSTANCE, fileText) as? PsiJavaFile ?: return null
            val topLevelClass = javaFile.classes.firstOrNull { it.name == segments.first() } ?: return null

            fun findDeclaration(element: PsiElement, segments: List<String>): PsiJavaDocumentedElement? {
                if (segments.isEmpty() && element is PsiJavaDocumentedElement) {
                    return element
                }

                assert(segments.isNotEmpty())
                val targetName = segments.first()

                if (element is PsiClass) {
                    val innerClass = element.innerClasses.firstOrNull { it.name == targetName }
                    if (innerClass != null) {
                        return findDeclaration(innerClass, segments.drop(1))
                    }

                    if (segments.size > 1) {
                        return null
                    }

                    val field = element.fields.firstOrNull { it.name == targetName }
                    if (field != null) {
                        return field
                    }

                    val method = element.methods.firstOrNull { it.name == targetName }
                    if (method != null) {
                        return method
                    }

                    return null
                }

                return null
            }

            val javadoc = findDeclaration(topLevelClass, segments.drop(1))?.docComment ?: return null
            return prettifyJavaDoc(javadoc.text)
        }
    }

    private fun prettifyKdoc(rawString: String): String {
        val withoutLeadingStars = rawString.trimMargin("*")

        return withoutLeadingStars
                .lines()
                .joinToString(LINE_SEPARATOR) { it.trim() }
                .replace(Regex("\n\n\n+"), "\n\n")
    }

    private fun prettifyJavaDoc(rawString: String): String {
        val withoutCommentSymbols = rawString.trim().substringAfter("/**").substringBeforeLast("*/").trim()
        val withoutHtmlTags = withoutCommentSymbols.replace(Regex("<[^A-Z>]*>"), "")
        val withoutLeadingStars = withoutHtmlTags.trimMargin("*")

        return withoutLeadingStars.lines()
                .map { it.trim() }
                .filter { line -> FILTERED_JAVA_TAGS.none { line.startsWith("@$it ") } }
                .joinToString(LINE_SEPARATOR)
                .replace(Regex("  +"), " ")
                .replace(Regex("\n\n\n+"), "\n\n")
    }
}