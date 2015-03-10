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

package org.jetbrains.kotlin.plugin.references

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.JetPsiFactory

public trait SimpleNameReferenceExtension {
    default object {
        public val EP_NAME: ExtensionPointName<SimpleNameReferenceExtension> = ExtensionPointName.create("org.jetbrains.kotlin.simpleNameReferenceExtension")!!
    }

    fun isReferenceTo(reference: JetSimpleNameReference, element: PsiElement): Boolean?
    fun handleElementRename(reference: JetSimpleNameReference, psiFactory: JetPsiFactory, newElementName: String): PsiElement?
}