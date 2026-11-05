/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterAST
import com.intellij.lang.LighterASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.LightStubElementFactory
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubSerializingElementFactory
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.stubs.KtStubElementFactories

/**
 * A base class for all factories.
 *
 * Each factory can build its stub both from the PSI/AST (see [createStub]) and from a light tree (see the
 * [LighterAST]-based [createStub] overload); the two paths must produce identical stubs because indexing builds
 * stubs from the light tree while other clients build them from the AST.
 */
internal abstract class KtStubSerializingElementFactory<Stub : StubElement<Psi>, Psi : KtElement>(
    protected val type: IElementType,
) : StubSerializingElementFactory<Stub, Psi>, LightStubElementFactory<Stub, Psi> {

    final override fun getExternalId(): String = "kotlin.$type"

    override fun indexStub(stub: Stub, sink: IndexSink) {
        // do not force inheritors to implement this method
    }

    /**
     * An element is stubbed only if its parent is stubbed as well.
     *
     * Overrides which stub an element regardless of its parent (e.g., local classes and functions) must be kept in sync with
     * [org.jetbrains.kotlin.psi.stubs.StubBuildingLexing.mayContainStubbedDeclarations], which allows the stub builder to skip
     * code blocks without such elements.
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

    /**
     * The light-tree mirror of [shouldCreateStub]: an element is stubbed only if its parent is stubbed as well.
     */
    override fun shouldCreateStub(tree: LighterAST, node: LighterASTNode, parentStub: StubElement<*>): Boolean {
        val parent = tree.getParent(node) ?: return false
        val parentType = parent.tokenType
        if (parentType is IFileElementType) {
            return true
        }

        val parentFactory = KtStubElementFactories[parentType]
        return parentFactory?.shouldCreateStub(tree, parent, parentStub) == true
    }
}
