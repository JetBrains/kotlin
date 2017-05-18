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
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.impl.light.LightJavaModule
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.resolve.jvm.modules.*

class IdeJavaModuleResolver(project: Project) : JavaModuleResolver {
    private val psiManager = PsiManager.getInstance(project)
    private val fileManager = JavaFileManager.SERVICE.getInstance(project)
    private val allScope = GlobalSearchScope.allScope(project)

    override val moduleGraph: JavaModuleGraph = JavaModuleGraph(
            object : JavaModuleFinder {
                override fun findModule(name: String): JavaModule? {
                    return fileManager.findModules(name, allScope).singleOrNull()?.toJavaModule()
                }
            }
    )

    override fun findJavaModule(file: VirtualFile): JavaModule? {
        val psiFile = psiManager.findFile(file) ?: return null
        return JavaModuleGraphUtil.findDescriptorByElement(psiFile)?.toJavaModule()
    }

    private fun PsiJavaModule.toJavaModule(): JavaModule {
        if (this is LightJavaModule) {
            return JavaModule.Automatic(name, rootVirtualFile)
        }

        val virtualFile = containingFile?.virtualFile ?: error("No VirtualFile found for module $this ($javaClass)")
        return JavaModule.Explicit(JavaModuleInfo.create(this), virtualFile.parent, virtualFile, this is PsiCompiledElement)
    }
}
