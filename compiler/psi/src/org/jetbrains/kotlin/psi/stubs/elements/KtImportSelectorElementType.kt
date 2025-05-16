/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.psi.KtImportInfo
import org.jetbrains.kotlin.psi.KtImportSelector
import org.jetbrains.kotlin.psi.stubs.KotlinImportSelectorStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinImportSelectorStubImpl

class KtImportSelectorElementType(debugName: String) :
    KtStubElementType<KotlinImportSelectorStub, KtImportSelector>(debugName, KtImportSelector::class.java, KotlinImportSelectorStub::class.java) {
    override fun createStub(psi: KtImportSelector, parentStub: StubElement<out PsiElement>?): KotlinImportSelectorStub {
        return KotlinImportSelectorStubImpl(parentStub, KtImportInfo.ImportSelector.Extension)
    }

    override fun serialize(stub: KotlinImportSelectorStub, dataStream: StubOutputStream) {
        when (stub.getSelector()) {
            KtImportInfo.ImportSelector.Extension -> dataStream.writeName("extension")
        }
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<PsiElement>?): KotlinImportSelectorStub {
        // we only have [extension] selectors for now
        return KotlinImportSelectorStubImpl(parentStub, KtImportInfo.ImportSelector.Extension)
    }
}
