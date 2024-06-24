/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import com.intellij.psi.SmartPointerManager
import org.jetbrains.kotlin.KtFakeSourceElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fakeElement

internal fun KtSourceElement.createPointer(): SourceElementPointer {
    return when (this) {
        is KtFakeSourceElement -> FakePsiSourceElementPointer(this)
        is KtRealPsiSourceElement -> RealPsiSourceElementPointer(this)
        else -> NonPsiSourceElementPointer
    }
}

internal interface SourceElementPointer {
    fun restore(): KtSourceElement?
}

private object NonPsiSourceElementPointer : SourceElementPointer {
    override fun restore(): KtSourceElement? {
        return null
    }
}

private class RealPsiSourceElementPointer(source: KtRealPsiSourceElement) : SourceElementPointer {
    private val psiPointer = SmartPointerManager.getInstance(source.psi.project).createSmartPsiElementPointer(source.psi)

    override fun restore(): KtSourceElement? {
        val psi = psiPointer.element ?: return null
        return KtRealPsiSourceElement(psi)
    }
}

private class FakePsiSourceElementPointer(source: KtFakeSourceElement) : SourceElementPointer {
    private val psiPointer = SmartPointerManager.getInstance(source.psi.project).createSmartPsiElementPointer(source.psi)
    private val kind = source.kind
    private val startOffset = source.startOffset
    private val endOffset = source.endOffset

    override fun restore(): KtSourceElement? {
        val element = psiPointer.element ?: return null
        return KtRealPsiSourceElement(element).fakeElement(kind, startOffset, endOffset)
    }
}