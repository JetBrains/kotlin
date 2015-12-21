/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.JavaProjectModelModificationService
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtils
import org.jetbrains.kotlin.idea.configuration.KotlinJavaModuleConfigurator
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator
import org.jetbrains.kotlin.idea.framework.JavaRuntimePresentationProvider
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.idea.versions.KotlinRuntimeLibraryUtil
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File

public class AddReflectionQuickFix(element: KtElement) : AddKotlinLibQuickFix(element) {
    override fun getText() = KotlinBundle.message("add.reflection.to.classpath")
    override fun getFamilyName() = text

    override fun libraryPath(): String = PathUtil.KOTLIN_JAVA_REFLECT_JAR
    override fun getLibFile(): File = PathUtil.getKotlinPathsForIdeaPlugin().reflectPath
    override fun hasLibJarInLibrary(library: Library): Boolean = JavaRuntimePresentationProvider.getReflectJar(library) != null
    override fun getLibraryDescriptor() = MavenExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-reflect")

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) = diagnostic.createIntentionForFirstParentOfType(::AddReflectionQuickFix)
    }
}

public class AddTestLibQuickFix(element: KtElement) : AddKotlinLibQuickFix(element) {
    override fun getText() = KotlinBundle.message("add.test.to.classpath")
    override fun getFamilyName() = text

    override fun libraryPath(): String = PathUtil.KOTLIN_TEST_JAR
    override fun getLibFile(): File = PathUtil.getKotlinPathsForIdeaPlugin().kotlinTestPath
    override fun hasLibJarInLibrary(library: Library): Boolean = JavaRuntimePresentationProvider.getTestJar(library) != null
    override fun getLibraryDescriptor() = MavenExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-test")

    companion object : KotlinSingleIntentionActionFactory() {
        val KOTLIN_TEST_UNRESOLVED = setOf(
                "Asserter", "assertFailsWith", "currentStackTrace", "failsWith", "todo", "assertEquals",
                "assertFails", "assertNot", "assertNotEquals", "assertNotNull", "assertNull", "assertTrue", "expect", "fail", "fails")

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val unresolvedReference = Errors.UNRESOLVED_REFERENCE.cast(diagnostic)

            if (unresolvedReference.a.text in KOTLIN_TEST_UNRESOLVED) {
                val ktFile = (diagnostic.psiElement.containingFile as? KtFile) ?: return null
                if (ktFile.importDirectives.none { it.text.contains("kotlin.test.") }) return null

                return diagnostic.createIntentionForFirstParentOfType(::AddTestLibQuickFix)
            }

            return null
        }
    }
}

public abstract class AddKotlinLibQuickFix(element: KtElement) : KotlinQuickFixAction<KtElement>(element) {
    protected abstract fun libraryPath(): String
    protected abstract fun getLibFile(): File
    protected abstract fun hasLibJarInLibrary(library: Library): Boolean
    protected abstract fun getLibraryDescriptor(): MavenExternalLibraryDescriptor

    class MavenExternalLibraryDescriptor(groupId: String, artifactId: String) : ExternalLibraryDescriptor(groupId, artifactId) {
        override fun getLibraryClassesRoots(): List<String> = emptyList()
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(element.containingFile.virtualFile)
        if (module != null && KotlinPluginUtil.isMavenModule(module)) {
            val scope = OrderEntryFix.suggestScopeByLocation(module, element)
            JavaProjectModelModificationService.getInstance(project).addDependency(module, getLibraryDescriptor(), scope)

            return
        }

        val libFile = getLibFile()
        if (!libFile.exists()) return

        val configurator = Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME)
                                   .firstIsInstanceOrNull<KotlinJavaModuleConfigurator>() ?: return

        for (library in KotlinRuntimeLibraryUtil.findKotlinLibraries(project)) {
            val runtimeJar = JavaRuntimePresentationProvider.getRuntimeJar(library) ?: continue
            if (hasLibJarInLibrary(library)) continue

            val model = library.modifiableModel

            val libFilesDir = VfsUtilCore.virtualToIoFile(runtimeJar).parent

            val libIoFile = File(libFilesDir, libraryPath())
            if (libIoFile.exists()) {
                model.addRoot(VfsUtil.getUrlForLibraryRoot(libIoFile), OrderRootType.CLASSES)
            }
            else {
                val copied = configurator.copyFileToDir(project, libFile, libFilesDir)!!
                model.addRoot(VfsUtil.getUrlForLibraryRoot(copied), OrderRootType.CLASSES)
            }

            model.commit()

            ConfigureKotlinInProjectUtils.showInfoNotification(
                    project, "${libraryPath()} was added to the library ${library.name}")
        }
    }
}
