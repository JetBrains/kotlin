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
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.stubs.impl.KotlinContextReceiverStubImpl
import org.jetbrains.kotlin.psi.stubs.light.NAME_REFERENCE_NAME_ELEMENT_TYPES
import org.jetbrains.kotlin.psi.stubs.light.firstChildOfType
import org.jetbrains.kotlin.psi.stubs.light.referencedName

internal object KtContextReceiverStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinContextReceiverStubImpl, KtContextReceiver>(
        type = KtNodeTypes.CONTEXT_RECEIVER,
    ) {

    override fun createPsi(stub: KotlinContextReceiverStubImpl): KtContextReceiver = KtContextReceiver(stub)

    override fun createStub(
        psi: KtContextReceiver,
        parentStub: StubElement<*>?,
    ): KotlinContextReceiverStubImpl = KotlinContextReceiverStubImpl(
        parent = parentStub,
        labelRef = StringRef.fromString(psi.labelName()),
    )

    override fun createStub(
        tree: LighterAST,
        node: LighterASTNode,
        parentStub: StubElement<*>,
    ): KotlinContextReceiverStubImpl = KotlinContextReceiverStubImpl(
        parent = parentStub,
        labelRef = StringRef.fromString(labelName(tree, node)),
    )

    /** Mirrors `KtContextReceiver.labelName`: the referenced name of the label inside the label qualifier. */
    private fun labelName(tree: LighterAST, node: LighterASTNode): String? {
        val labelQualifier = tree.firstChildOfType(node, KtNodeTypes.LABEL_QUALIFIER) ?: return null
        val label = tree.firstChildOfType(labelQualifier, KtNodeTypes.LABEL) ?: return null
        return tree.referencedName(label, NAME_REFERENCE_NAME_ELEMENT_TYPES)
    }

    override fun serialize(stub: KotlinContextReceiverStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.label)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinContextReceiverStubImpl = KotlinContextReceiverStubImpl(
        parent = parentStub,
        labelRef = dataStream.readName(),
    )
}
