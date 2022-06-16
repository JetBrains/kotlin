/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.KtContextReceiverList
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl

internal class ContextReceiversListStubBuilder(c: ClsStubBuilderContext) {
    private val typeStubBuilder = TypeClsStubBuilder(c)

    fun createContextReceiverStubs(parent: StubElement<*>, contextReceiverTypes: List<ProtoBuf.Type>) {
        if (contextReceiverTypes.isEmpty()) return
        val contextReceiverListStub =
            KotlinPlaceHolderStubImpl<KtContextReceiverList>(parent, KtStubElementTypes.CONTEXT_RECEIVER_LIST)
        for (contextReceiverType in contextReceiverTypes) {
            val contextReceiverStub =
                KotlinPlaceHolderStubImpl<KtContextReceiver>(contextReceiverListStub, KtStubElementTypes.CONTEXT_RECEIVER)
            typeStubBuilder.createTypeReferenceStub(contextReceiverStub, contextReceiverType)
        }
    }
}