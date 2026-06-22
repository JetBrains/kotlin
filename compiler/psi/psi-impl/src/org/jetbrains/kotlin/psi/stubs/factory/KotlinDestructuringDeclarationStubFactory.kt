/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.KotlinDestructuringDeclarationStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinDestructuringDeclarationStubFactory :
    StubElementFactory<KotlinDestructuringDeclarationStubImpl, KtDestructuringDeclaration> {
    override fun shouldCreateStub(node: ASTNode): Boolean {
        val parent = node.treeParent
        return when (parent?.elementType) {
            KtFileElementType, KtStubElementTypes.CLASS_BODY -> true
            KtNodeTypes.BLOCK -> parent.treeParent?.elementType == KtStubElementTypes.SCRIPT
            else -> false
        }
    }

    override fun createStub(
        psi: KtDestructuringDeclaration,
        parentStub: StubElement<out PsiElement>?,
    ): KotlinDestructuringDeclarationStubImpl {
        return KotlinDestructuringDeclarationStubImpl(
            parent = parentStub,
            isVar = psi.isVar,
            hasInitializer = psi.hasInitializer(),
        )
    }

    override fun createPsi(stub: KotlinDestructuringDeclarationStubImpl): KtDestructuringDeclaration =
        KtDestructuringDeclaration(stub)
}

internal object KotlinDestructuringDeclarationStubSerializer : StubSerializer<KotlinDestructuringDeclarationStubImpl> {
    override fun getExternalId(): String = "kotlin.DESTRUCTURING_DECLARATION"

    override fun serialize(stub: KotlinDestructuringDeclarationStubImpl, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.isVar)
        dataStream.writeBoolean(stub.hasInitializer)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinDestructuringDeclarationStubImpl {
        return KotlinDestructuringDeclarationStubImpl(
            parent = parentStub,
            isVar = dataStream.readBoolean(),
            hasInitializer = dataStream.readBoolean(),
        )
    }

    override fun indexStub(stub: KotlinDestructuringDeclarationStubImpl, sink: IndexSink) {
        // not indexed
    }
}
