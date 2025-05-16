/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtImportInfo
import org.jetbrains.kotlin.psi.KtImportSelector
import org.jetbrains.kotlin.psi.stubs.KotlinImportSelectorStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinImportSelectorStubImpl(
    parent: StubElement<out PsiElement>?,
    private val selector: KtImportInfo.ImportSelector
) : KotlinStubBaseImpl<KtImportSelector>(parent, KtStubElementTypes.IMPORT_SELECTOR), KotlinImportSelectorStub {
    override fun getSelector(): KtImportInfo.ImportSelector = selector
}
