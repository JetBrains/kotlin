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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
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

public class AddReflectionQuickFix(element: KtElement) : KotlinQuickFixAction<KtElement>(element) {
    override fun getText() = KotlinBundle.message("add.reflection.to.classpath")
    override fun getFamilyName() = getText()

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val pluginReflectJar = PathUtil.getKotlinPathsForIdeaPlugin().getReflectPath()
        if (!pluginReflectJar.exists()) return

        val configurator = Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME)
                                   .firstIsInstanceOrNull<KotlinJavaModuleConfigurator>() ?: return

        for (library in KotlinRuntimeLibraryUtil.findKotlinLibraries(project)) {
            val runtimeJar = JavaRuntimePresentationProvider.getRuntimeJar(library) ?: continue
            if (JavaRuntimePresentationProvider.getReflectJar(library) != null) continue

            val model = library.getModifiableModel()

            val libFilesDir = VfsUtilCore.virtualToIoFile(runtimeJar).getParent()
            val reflectIoFile = File(libFilesDir, PathUtil.KOTLIN_JAVA_REFLECT_JAR)
            if (reflectIoFile.exists()) {
                model.addRoot(VfsUtil.getUrlForLibraryRoot(reflectIoFile), OrderRootType.CLASSES)
            }
            else {
                val copied = configurator.copyFileToDir(project, pluginReflectJar, libFilesDir)!!
                model.addRoot(VfsUtil.getUrlForLibraryRoot(copied), OrderRootType.CLASSES)
            }

            model.commit()

            ConfigureKotlinInProjectUtils.showInfoNotification(project,
                    "${PathUtil.KOTLIN_JAVA_REFLECT_JAR} was added to the library ${library.getName()}"
            )
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) = diagnostic.createIntentionForFirstParentOfType(::AddReflectionQuickFix)
    }
}
