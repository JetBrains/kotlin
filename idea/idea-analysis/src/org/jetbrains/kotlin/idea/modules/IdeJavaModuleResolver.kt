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

package org.jetbrains.kotlin.idea.modules

import com.intellij.codeInsight.daemon.impl.analysis.JavaModuleGraphUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.light.LightJavaModule
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

class IdeJavaModuleResolver(private val project: Project) : JavaModuleResolver {
    private fun findJavaModule(file: VirtualFile): PsiJavaModule? =
            ModuleHighlightUtil2.getModuleDescriptor(file, project)

    override fun checkAccessibility(
            fileFromOurModule: VirtualFile?, referencedFile: VirtualFile, referencedPackage: FqName?
    ): JavaModuleResolver.AccessError? {
        val ourModule = fileFromOurModule?.let(this::findJavaModule)
        val theirModule = findJavaModule(referencedFile)

        if (ourModule?.name == theirModule?.name) return null

        if (theirModule == null) {
            return JavaModuleResolver.AccessError.ModuleDoesNotReadUnnamedModule
        }

        if (ourModule != null && !JavaModuleGraphUtil.reads(ourModule, theirModule)) {
            return JavaModuleResolver.AccessError.ModuleDoesNotReadModule(theirModule.name)
        }

        val fqName = referencedPackage?.asString() ?: return null
        if (!exports(theirModule, fqName, ourModule)) {
            return JavaModuleResolver.AccessError.ModuleDoesNotExportPackage(theirModule.name)
        }

        return null
    }

    // Returns whether or not [source] exports [packageName] to [target]
    private fun exports(source: PsiJavaModule, packageName: String, target: PsiJavaModule?): Boolean {
        if (source is LightJavaModule) {
            return true
        }

        // TODO: simply call JavaModuleGraphUtil.exports as soon as its 'target' parameter is nullable
        if (target != null) {
            return JavaModuleGraphUtil.exports(source, packageName, target)
        }
        return source.exports.any { statement ->
            statement.moduleNames.isEmpty() && statement.packageName == packageName
        }
    }
}
