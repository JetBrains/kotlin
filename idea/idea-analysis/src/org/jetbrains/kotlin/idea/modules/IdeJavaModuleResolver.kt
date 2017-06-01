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

//import com.intellij.codeInsight.daemon.impl.analysis.JavaModuleGraphUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
//import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiManager
//import com.intellij.psi.impl.light.LightJavaModule
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

class IdeJavaModuleResolver(project: Project) : JavaModuleResolver {
    /*private val psiManager = PsiManager.getInstance(project)

    private fun findJavaModule(file: VirtualFile): PsiJavaModule? {
        return psiManager.findFile(file)?.let(JavaModuleGraphUtil::findDescriptorByElement)
    }*/

    override fun checkAccessibility(
            fileFromOurModule: VirtualFile?, referencedFile: VirtualFile, referencedPackage: FqName?
    ): JavaModuleResolver.AccessError? {
        /*val ourModule = fileFromOurModule?.let(this::findJavaModule)
        val theirModule = this.findJavaModule(referencedFile)

        // If we're both in the unnamed module, it's OK, no error should be reported
        if (ourModule == null && theirModule == null) return null

        if (theirModule == null) {
            // We should probably prohibit this usage according to JPMS (named module cannot use types from unnamed module),
            // but we cannot be sure that a module without module-info.java is going to be actually used as an unnamed module.
            // It could also be an automatic module, in which case it would be read by every module.
            return null
        }

        if (ourModule?.name == theirModule.name) return null

        if (ourModule != null && !JavaModuleGraphUtil.reads(ourModule, theirModule)) {
            return JavaModuleResolver.AccessError.ModuleDoesNotReadModule(theirModule.name)
        }

        val fqName = referencedPackage?.asString() ?: return null
        if (!exports(theirModule, fqName, ourModule)) {
            return JavaModuleResolver.AccessError.ModuleDoesNotExportPackage(theirModule.name)
        }*/

        return null
    }

    // Returns whether or not [source] exports [packageName] to [target]
    /*private fun exports(source: PsiJavaModule, packageName: String, target: PsiJavaModule?): Boolean {
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
    }*/
}
