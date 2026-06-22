/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.DataInputOutputUtil
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinModifierListStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.ModifierMaskUtils.computeMaskFromModifierList

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinModifierListStubFactory : StubElementFactory<KotlinModifierListStubImpl, KtDeclarationModifierList> {
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(psi: KtDeclarationModifierList, parentStub: StubElement<out PsiElement>?): KotlinModifierListStubImpl {
        return KotlinModifierListStubImpl(parentStub, computeMaskFromModifierList(psi))
    }

    override fun createPsi(stub: KotlinModifierListStubImpl): KtDeclarationModifierList = KtDeclarationModifierList(stub)
}

internal object KotlinModifierListStubSerializer : StubSerializer<KotlinModifierListStubImpl> {
    override fun getExternalId(): String = "kotlin.MODIFIER_LIST"

    override fun serialize(stub: KotlinModifierListStubImpl, dataStream: StubOutputStream) {
        DataInputOutputUtil.writeLONG(dataStream, stub.mask)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinModifierListStubImpl {
        val mask = DataInputOutputUtil.readLONG(dataStream)
        return KotlinModifierListStubImpl(parentStub, mask)
    }

    override fun indexStub(stub: KotlinModifierListStubImpl, sink: IndexSink) {
        // not indexed
    }
}
