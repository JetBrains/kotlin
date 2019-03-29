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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.isJvm

class DuplicateJvmSignatureAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is KtFile && element !is KtDeclaration) return
        if (!ProjectRootsUtil.isInProjectSource(element)) return

        val file = element.containingFile
        if (file !is KtFile || !TargetPlatformDetector.getPlatform(file).isJvm()) return

        val otherDiagnostics = when (element) {
            is KtDeclaration -> element.analyzeWithContent()
            is KtFile -> element.analyzeWithContent()
            else -> throw AssertionError("DuplicateJvmSignatureAnnotator: should not get here! Element: ${element.text}")
        }.diagnostics

        val moduleScope = element.getModuleInfo().contentScope()
        val diagnostics = getJvmSignatureDiagnostics(element, otherDiagnostics, moduleScope) ?: return

        KotlinPsiChecker().annotateElement(element, holder, diagnostics)
    }
}
