/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.LighterAST
import com.intellij.lang.LighterASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeKdocText
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPrimaryConstructorStubImpl
import org.jetbrains.kotlin.psi.stubs.light.anyDeclarationName
import org.jetbrains.kotlin.psi.stubs.light.containingClassOrObject

internal object KtPrimaryConstructorStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinPrimaryConstructorStubImpl, KtPrimaryConstructor>(
        type = KtNodeTypes.PRIMARY_CONSTRUCTOR,
    ) {

    override fun createPsi(stub: KotlinPrimaryConstructorStubImpl): KtPrimaryConstructor = KtPrimaryConstructor(stub)

    override fun createStub(
        psi: KtPrimaryConstructor,
        parentStub: StubElement<*>?,
    ): KotlinPrimaryConstructorStubImpl = KotlinPrimaryConstructorStubImpl(
        parent = parentStub,
        containingClassName = StringRef.fromString(psi.name),
        kdocText = null,
    )

    override fun createStub(
        tree: LighterAST,
        node: LighterASTNode,
        parentStub: StubElement<*>,
    ): KotlinPrimaryConstructorStubImpl = KotlinPrimaryConstructorStubImpl(
        parent = parentStub,
        // Mirrors `KtConstructor.getName`, which is the containing class name
        containingClassName = StringRef.fromString(
            tree.containingClassOrObject(node)?.let { tree.anyDeclarationName(it) },
        ),
        kdocText = null,
    )

    override fun serialize(stub: KotlinPrimaryConstructorStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.serializeKdocText(stub.kdocText)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinPrimaryConstructorStubImpl {
        val name = dataStream.readName()
        val kdocText = dataStream.deserializeKdocText()
        return KotlinPrimaryConstructorStubImpl(
            parent = parentStub,
            containingClassName = name,
            kdocText = kdocText,
        )
    }
}
