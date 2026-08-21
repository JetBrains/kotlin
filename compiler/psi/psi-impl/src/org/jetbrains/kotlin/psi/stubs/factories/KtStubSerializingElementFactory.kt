/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubSerializingElementFactory
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.stubs.KtStubElementFactories

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

    /**
     * An element is stubbed only if its parent is stubbed as well.
     */
    override fun shouldCreateStub(node: ASTNode): Boolean {
        val parent = node.treeParent
        val parentType = parent.elementType
        if (parentType is IFileElementType) {
            return true
        }

        val parentFactory = KtStubElementFactories[parentType]
        return parentFactory?.shouldCreateStub(parent) == true
    }
}
