/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(SuspiciousFakeSourceCheck::class)

package org.jetbrains.kotlin.analysis.api.fir.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.*

internal fun KtSourceElement.createPointer(): SourceElementPointer = when (this) {
    is KtRealPsiSourceElement -> RealPsiSourceElementPointer(this)
    is KtFakePsiSourceElementWithCustomOffsetStrategy -> FakePsiSourceElementPointerWithCustomOffsetStrategy(this)
    is KtFakePsiSourceElement -> FakePsiSourceElementPointer(this)
    else -> NonPsiSourceElementPointer
}

internal interface SourceElementPointer {
    fun restore(): KtSourceElement?
}

private object NonPsiSourceElementPointer : SourceElementPointer {
    override fun restore(): KtSourceElement? {
        return null
    }
}

private val KtPsiSourceElement.smartPsiPointer: SmartPsiElementPointer<PsiElement>
    get() = SmartPointerManager.getInstance(psi.project).createSmartPsiElementPointer(psi)

private class RealPsiSourceElementPointer(source: KtRealPsiSourceElement) : SourceElementPointer {
    private val psiPointer = source.smartPsiPointer

    override fun restore(): KtSourceElement? {
        val psi = psiPointer.element ?: return null
        return KtRealPsiSourceElement(psi)
    }
}

private class FakePsiSourceElementPointer(source: KtFakePsiSourceElement) : SourceElementPointer {
    private val psiPointer = source.smartPsiPointer
    private val kind = source.kind

    override fun restore(): KtSourceElement? {
        val element = psiPointer.element ?: return null
        return KtRealPsiSourceElement(element).fakeElement(kind)
    }
}

private class FakePsiSourceElementPointerWithCustomOffsetStrategy(
    source: KtFakePsiSourceElementWithCustomOffsetStrategy,
) : SourceElementPointer {
    private val psiPointer = source.smartPsiPointer
    private val kind = source.kind
    private val customStrategyPointer = source.strategy.createPointer()

    override fun restore(): KtSourceElement? {
        val element = psiPointer.element ?: return null
        val strategy = customStrategyPointer.restore() ?: return null
        return KtRealPsiSourceElement(element).fakeElement(kind, strategy)
    }
}

private fun interface OffsetStrategyPointer {
    fun restore(): KtSourceElementOffsetStrategy.Custom?
}

private fun KtSourceElementOffsetStrategy.Custom.createPointer(): OffsetStrategyPointer = when (this) {
    is KtSourceElementOffsetStrategy.Custom.Delegated -> {
        val startOffsetAnchorPointer = startOffsetAnchor.createPointer()
        val endOffsetAnchorPointer = endOffsetAnchor.createPointer()
        OffsetStrategyPointer {
            startOffsetAnchorPointer.restore()?.let { startOffsetAnchor ->
                endOffsetAnchorPointer.restore()?.let { endOffsetAnchor ->
                    KtSourceElementOffsetStrategy.Custom.Delegated(startOffsetAnchor, endOffsetAnchor)
                }
            }
        }
    }

    // no psi inside -> can be safely stored as is
    is KtSourceElementOffsetStrategy.Custom.Initialized -> OffsetStrategyPointer { this }
}
