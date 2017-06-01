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

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*

class MockExternalAnnotationsManager : ExternalAnnotationsManager() {
    override fun chooseAnnotationsPlace(element: PsiElement): AnnotationPlace? = null

    override fun isExternalAnnotationWritable(listOwner: PsiModifierListOwner, annotationFQN: String): Boolean = false
    override fun isExternalAnnotation(annotation: PsiAnnotation): Boolean = false

    override fun findExternalAnnotationsFiles(listOwner: PsiModifierListOwner): List<PsiFile>? = null
    override fun findExternalAnnotation(listOwner: PsiModifierListOwner, annotationFQN: String): PsiAnnotation? = null
    override fun findExternalAnnotations(listOwner: PsiModifierListOwner): Array<out PsiAnnotation>? = null

    override fun annotateExternally(listOwner: PsiModifierListOwner, annotationFQName: String, fromFile: PsiFile, value: Array<out PsiNameValuePair>?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun deannotate(listOwner: PsiModifierListOwner, annotationFQN: String): Boolean {
        throw UnsupportedOperationException("not implemented")
    }

    override fun editExternalAnnotation(listOwner: PsiModifierListOwner, annotationFQN: String, value: Array<out PsiNameValuePair>?): Boolean {
        throw UnsupportedOperationException("not implemented")
    }

    override fun hasAnnotationRootsForFile(file: VirtualFile): Boolean {
        throw UnsupportedOperationException("not implemented")
    }
}