/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubSerializingElementFactory
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType

/**
 * A base class for all factories.
 */
internal abstract class KtStubSerializingElementFactory<Stub : StubElement<Psi>, Psi : KtElement>(
    protected val type: IElementType,
) : StubSerializingElementFactory<Stub, Psi> {

    final override fun getExternalId(): String = "kotlin.$type"

    override fun indexStub(stub: Stub, sink: IndexSink) {
        // do not force inheritors to implement this method
    }

    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.createStubDependingOnParent(node)
}
