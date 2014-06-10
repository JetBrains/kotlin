/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.caches.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.resolve.java.jetAsJava.KotlinLightElement
import org.jetbrains.jet.lang.psi.*
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.LibraryOrSdkOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.JdkOrderEntry
import org.jetbrains.jet.asJava.FakeLightClassForFileOfPackage
import org.jetbrains.jet.asJava.KotlinLightClassForPackage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

fun PsiElement.getModuleInfo(): IdeaModuleInfo {
    fun logAndReturnDefault(message: String): IdeaModuleInfo {
        LOG.error("Could not find correct module information.\nReason: $message")
        return NotUnderContentRootModuleInfo
    }

    if (this is KotlinLightElement<*, *>)
        return this.getModuleInfoForLightElement()

    if (this is JetCodeFragment)
        return this.getContext()?.getModuleInfo()
                ?: logAndReturnDefault("Analyzing code fragment of type $javaClass with no context element\nText:\n${getText()}")

    val containingJetFile = (this as? JetElement)?.getContainingFile() as? JetFile
    val context = containingJetFile?.analysisContext
    if (context != null) return context.getModuleInfo()

    val doNotAnalyze = containingJetFile?.doNotAnalyze
    if (doNotAnalyze != null) {
        return logAndReturnDefault(
                "Should not analyze element: ${getText()} in file ${containingJetFile?.getName() ?: " <no file>"}\n$doNotAnalyze"
        )
    }

    val project = getProject()
    val containingFile = getContainingFile()
            ?: return logAndReturnDefault("Analyzing element of type $javaClass with no containing file\nText:\n${getText()}")

    val virtualFile = containingFile.getOriginalFile().getVirtualFile()
            ?: return logAndReturnDefault("Analyzing non-physical file $containingFile of type ${containingFile.javaClass}")

    return getModuleInfoByVirtualFile(project, virtualFile)
}

private fun getModuleInfoByVirtualFile(project: Project, virtualFile: VirtualFile): IdeaModuleInfo {
    val projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project)

    val module = projectFileIndex.getModuleForFile(virtualFile)
    if (module != null) return module.toSourceInfo()

    val orderEntries = projectFileIndex.getOrderEntriesForFile(virtualFile)

    val libraryOrSdkEntries = orderEntries.filterIsInstance(javaClass<LibraryOrSdkOrderEntry>())
    @entries for (libraryOrSdkOrderEntry in libraryOrSdkEntries) {
        when (libraryOrSdkOrderEntry) {
            is LibraryOrderEntry -> {
                val library = libraryOrSdkOrderEntry.getLibrary() ?: continue @entries
                if (projectFileIndex.isInLibrarySource(virtualFile)) {
                    return LibrarySourceInfo(project, library)
                }
                else {
                    return LibraryInfo(project, library)
                }
            }
            is JdkOrderEntry -> {
                val sdk = libraryOrSdkOrderEntry.getJdk() ?: continue @entries
                return SdkInfo(project, sdk)
            }
        }
    }
    return NotUnderContentRootModuleInfo
}

private fun KotlinLightElement<*, *>.getModuleInfoForLightElement(): IdeaModuleInfo {
    val element = origin ?: when (this) {
        is FakeLightClassForFileOfPackage -> this.getContainingFile()!!
        is KotlinLightClassForPackage -> this.getFiles().first()
        else -> throw IllegalStateException("Unknown light class without origin is referenced by IDE lazy resolve: $javaClass")
    }
    return element.getModuleInfo()
}