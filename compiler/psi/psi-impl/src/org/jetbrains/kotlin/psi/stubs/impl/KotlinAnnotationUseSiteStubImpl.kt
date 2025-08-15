/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtAnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationUseSiteTargetStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinAnnotationUseSiteTargetStubImpl(
    parent: StubElement<out PsiElement>?,
    private val target: StringRef
) : KotlinStubBaseImpl<KtAnnotationUseSiteTarget>(parent, KtStubElementTypes.ANNOTATION_TARGET), KotlinAnnotationUseSiteTargetStub {
    override val useSiteTarget: String
        get() = target.string
}
