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

import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.JavaProjectModelModificationService
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.configuration.KotlinJavaModuleConfigurator
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator
import org.jetbrains.kotlin.idea.configuration.KotlinWithGradleConfigurator
import org.jetbrains.kotlin.idea.configuration.createConfigureKotlinNotificationCollector
import org.jetbrains.kotlin.idea.framework.getReflectJar
import org.jetbrains.kotlin.idea.framework.getRuntimeJar
import org.jetbrains.kotlin.idea.framework.getTestJar
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.kotlin.idea.versions.findAllUsedLibraries
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File

class AddReflectionQuickFix(element: KtElement) : AddKotlinLibQuickFix(element) {
    override fun getText() = KotlinBundle.message("add.reflection.to.classpath")
    override fun getFamilyName() = text

    override fun libraryPath(): String = PathUtil.KOTLIN_JAVA_REFLECT_JAR
    override fun getLibFile(): File = PathUtil.getKotlinPathsForIdeaPlugin().reflectPath
    override fun hasLibJarInLibrary(library: Library): Boolean = getReflectJar(library) != null
    override fun getLibraryDescriptor(module: Module) = MavenExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-reflect",
                                                                                       AddKotlinLibQuickFix.getKotlinStdlibVersion(module))

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) = diagnostic.createIntentionForFirstParentOfType(::AddReflectionQuickFix)
    }
}

class AddTestLibQuickFix(element: KtElement) : AddKotlinLibQuickFix(element) {
    override fun getText() = KotlinBundle.message("add.test.to.classpath")
    override fun getFamilyName() = text

    override fun libraryPath(): String = PathUtil.KOTLIN_TEST_JAR
    override fun getLibFile(): File = PathUtil.getKotlinPathsForIdeaPlugin().kotlinTestPath
    override fun hasLibJarInLibrary(library: Library): Boolean = getTestJar(library) != null
    override fun getLibraryDescriptor(module: Module) = MavenExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-test",
                                                                                       AddKotlinLibQuickFix.getKotlinStdlibVersion(module))

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

abstract class AddKotlinLibQuickFix(element: KtElement) : KotlinQuickFixAction<KtElement>(element) {
    protected abstract fun libraryPath(): String
    protected abstract fun getLibFile(): File
    protected abstract fun hasLibJarInLibrary(library: Library): Boolean
    protected abstract fun getLibraryDescriptor(module: Module): MavenExternalLibraryDescriptor

    class MavenExternalLibraryDescriptor(groupId: String, artifactId: String, version: String) :
            ExternalLibraryDescriptor(groupId, artifactId, version, version) {
        override fun getLibraryClassesRoots(): List<String> = emptyList()
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(element.containingFile.virtualFile)
        if (module != null) {
            if (KotlinPluginUtil.isMavenModule(module)) {
                val scope = OrderEntryFix.suggestScopeByLocation(module, element)
                JavaProjectModelModificationService.getInstance(project).addDependency(module, getLibraryDescriptor(module), scope)

                return
            }

            if (KotlinPluginUtil.isGradleModule(module) || KotlinPluginUtil.isAndroidGradleModule(module)) {
                val scope = OrderEntryFix.suggestScopeByLocation(module, element)
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(module, scope, getLibraryDescriptor(module))

                return
            }
        }

        val libFile = getLibFile()
        if (!libFile.exists()) return

        val configurator = Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME)
                                   .firstIsInstanceOrNull<KotlinJavaModuleConfigurator>() ?: return

        val collector = createConfigureKotlinNotificationCollector(project)

        for (library in findAllUsedLibraries(project).keySet()) {
            val runtimeJar = getRuntimeJar(library) ?: continue
            if (hasLibJarInLibrary(library)) continue

            val model = library.modifiableModel

            val libFilesDir = VfsUtilCore.virtualToIoFile(runtimeJar).parent

            val libIoFile = File(libFilesDir, libraryPath())
            if (libIoFile.exists()) {
                model.addRoot(VfsUtil.getUrlForLibraryRoot(libIoFile), OrderRootType.CLASSES)
            }
            else {
                val copied = configurator.copyFileToDir(libFile, libFilesDir, collector)!!
                model.addRoot(VfsUtil.getUrlForLibraryRoot(copied), OrderRootType.CLASSES)
            }

            model.commit()
        }

        collector.showNotification()
    }

    companion object {
        fun getKotlinStdlibVersion(module: Module): String {
            if (KotlinPluginUtil.isMavenModule(module)) {
                val mavenVersion = getMavenKotlinStdlibVersion(module)
                if (mavenVersion != null) {
                    return mavenVersion
                }
            }
            else if (KotlinPluginUtil.isGradleModule(module) || KotlinPluginUtil.isAndroidGradleModule(module)) {
                val gradleVersion = KotlinWithGradleConfigurator.getKotlinStdlibVersion(module)
                if (gradleVersion != null) {
                    return gradleVersion
                }
            }

            val pluginVersion = bundledRuntimeVersion()
            if ("@snapshot@" == pluginVersion) {
                return "1.1-SNAPSHOT"
            }
            return pluginVersion
        }

        fun getMavenKotlinStdlibVersion(module: Module): String? {
            LibrariesContainerFactory.createContainer(module).allLibraries.forEach { library ->
                val libName = library.name
                if (libName != null && libName.contains("org.jetbrains.kotlin:kotlin-stdlib")) {
                    return libName.substringAfterLast(":")
                }
            }

            return null
        }
    }
}