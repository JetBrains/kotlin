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

package org.jetbrains.kotlin.android.intention

import com.intellij.openapi.editor.Editor
import org.jetbrains.android.dom.manifest.Manifest
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.android.insideBody
import org.jetbrains.kotlin.idea.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.isAbstract
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.isProtected


abstract class AbstractRegisterComponentAction(text: String) : SelfTargetingIntention<KtClass>(KtClass::class.java, text) {

    abstract fun isApplicableTo(element: KtClass, manifest: Manifest): Boolean

    abstract fun applyTo(element: KtClass, manifest: Manifest)

    final override fun isApplicableTo(element: KtClass, caretOffset: Int): Boolean {
        val androidFacet = AndroidFacet.getInstance(element.containingFile) ?: return false
        val manifest = androidFacet.manifest ?: return false
        return !element.isLocal &&
               !element.isAbstract() &&
               !element.isPrivate() &&
               !element.isProtected() &&
               !element.isInner() &&
               !element.name.isNullOrEmpty() &&
               !element.insideBody(caretOffset) &&
               isApplicableTo(element, manifest)
    }

    final override fun applyTo(element: KtClass, editor: Editor?) {
        AndroidFacet.getInstance(element.containingFile)?.manifest?.let {
            applyTo(element, it)
        }
    }
}