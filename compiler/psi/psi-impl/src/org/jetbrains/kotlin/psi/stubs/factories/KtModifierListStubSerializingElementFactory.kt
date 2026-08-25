/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.DataInputOutputUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinModifierListStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.ModifierMaskUtils

internal object KtModifierListStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinModifierListStubImpl, KtDeclarationModifierList>(
        type = KtNodeTypes.MODIFIER_LIST,
    ) {

    override fun createPsi(
        stub: KotlinModifierListStubImpl,
    ): KtDeclarationModifierList = KtDeclarationModifierList(stub)

    override fun createStub(
        psi: KtDeclarationModifierList,
        parentStub: StubElement<*>?,
    ): KotlinModifierListStubImpl = KotlinModifierListStubImpl(
        parentStub,
        ModifierMaskUtils.computeMaskFromModifierList(psi),
    )

    override fun serialize(stub: KotlinModifierListStubImpl, dataStream: StubOutputStream) {
        DataInputOutputUtil.writeLONG(dataStream, stub.mask)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinModifierListStubImpl = KotlinModifierListStubImpl(
        parentStub,
        /* mask = */ DataInputOutputUtil.readLONG(dataStream),
    )
}
