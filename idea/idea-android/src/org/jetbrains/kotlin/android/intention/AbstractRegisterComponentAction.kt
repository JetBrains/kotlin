/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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