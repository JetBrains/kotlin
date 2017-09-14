/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.configuration.findApplicableConfigurator
import org.jetbrains.kotlin.idea.facet.getRuntimeLibraryVersion
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.idea.versions.LibraryJarDescriptor
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective

class AddReflectionQuickFix(element: KtElement)
        : AddKotlinLibQuickFix(element, listOf(LibraryJarDescriptor.REFLECT_JAR,
                                               LibraryJarDescriptor.REFLECT_SRC_JAR)) {
    override fun getText() = KotlinBundle.message("add.reflection.to.classpath")
    override fun getFamilyName() = text

    override fun getLibraryDescriptor(module: Module) = MavenExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-reflect",
                                                                                       getRuntimeLibraryVersion(module) ?: bundledRuntimeVersion())

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) = diagnostic.createIntentionForFirstParentOfType(::AddReflectionQuickFix)
    }
}

class AddTestLibQuickFix(element: KtElement)
        : AddKotlinLibQuickFix(element, listOf(LibraryJarDescriptor.TEST_JAR,
                                               LibraryJarDescriptor.TEST_SRC_JAR)) {
    override fun getText() = KotlinBundle.message("add.test.to.classpath")
    override fun getFamilyName() = text

    override fun getLibraryDescriptor(module: Module) = MavenExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-test",
                                                                                       getRuntimeLibraryVersion(module) ?: bundledRuntimeVersion())

    companion object : KotlinSingleIntentionActionFactory() {
        val KOTLIN_TEST_UNRESOLVED = setOf(
                "Asserter", "assertFailsWith", "currentStackTrace", "failsWith", "todo", "assertEquals",
                "assertFails", "assertNot", "assertNotEquals", "assertNotNull", "assertNull", "assertTrue", "expect", "fail", "fails")

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val unresolvedReference = Errors.UNRESOLVED_REFERENCE.cast(diagnostic)

            if (PsiTreeUtil.getParentOfType(diagnostic.psiElement, KtImportDirective::class.java) != null) return null

            val unresolvedText = unresolvedReference.a.text
            if (unresolvedText in KOTLIN_TEST_UNRESOLVED) {
                val ktFile = (diagnostic.psiElement.containingFile as? KtFile) ?: return null

                val exactImportFqName = FqName("kotlin.test.$unresolvedText")
                val kotlinTestAllUnder = FqName("kotlin.test")

                var hasExactImport = false
                var hasKotlinTestAllUnder = false

                for (importDirective in ktFile.importDirectives.filter { it.text.contains("kotlin.test.") }) {
                    if (importDirective.importedFqName == exactImportFqName) {
                        hasExactImport = true
                        break
                    }

                    if (importDirective.importedFqName == kotlinTestAllUnder && importDirective.isAllUnder) {
                        hasKotlinTestAllUnder = true
                        break
                    }
                }

                if (hasExactImport || hasKotlinTestAllUnder) {
                    return diagnostic.createIntentionForFirstParentOfType(::AddTestLibQuickFix)
                }

            }

            return null
        }
    }
}

abstract class AddKotlinLibQuickFix(element: KtElement,
                                    val libraryJarDescriptors: List<LibraryJarDescriptor>) : KotlinQuickFixAction<KtElement>(element) {
    protected abstract fun getLibraryDescriptor(module: Module): MavenExternalLibraryDescriptor

    class MavenExternalLibraryDescriptor(groupId: String, artifactId: String, version: String) :
            ExternalLibraryDescriptor(groupId, artifactId, version, version) {
        override fun getLibraryClassesRoots(): List<String> = emptyList()
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(element.containingFile.virtualFile) ?: return

        val configurator = findApplicableConfigurator(module)
        configurator.addLibraryDependency(module, element, getLibraryDescriptor(module), libraryJarDescriptors)
    }
}