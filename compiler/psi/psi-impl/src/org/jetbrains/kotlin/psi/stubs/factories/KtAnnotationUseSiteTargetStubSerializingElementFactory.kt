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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.stubs.impl.KotlinAnnotationUseSiteTargetStubImpl
import org.jetbrains.kotlin.psi.stubs.light.nodeText

internal object KtAnnotationUseSiteTargetStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinAnnotationUseSiteTargetStubImpl, KtAnnotationUseSiteTarget>(
        type = KtNodeTypes.ANNOTATION_TARGET,
    ) {

    override fun createPsi(
        stub: KotlinAnnotationUseSiteTargetStubImpl,
    ): KtAnnotationUseSiteTarget = KtAnnotationUseSiteTarget(stub)

    override fun createStub(
        psi: KtAnnotationUseSiteTarget,
        parentStub: StubElement<*>?,
    ): KotlinAnnotationUseSiteTargetStubImpl = KotlinAnnotationUseSiteTargetStubImpl(
        parent = parentStub,
        useSiteTargetRef = StringRef.fromString(psi.getAnnotationUseSiteTarget().name)!!,
    )

    override fun createStub(
        tree: LighterAST,
        node: LighterASTNode,
        parentStub: StubElement<*>,
    ): KotlinAnnotationUseSiteTargetStubImpl = KotlinAnnotationUseSiteTargetStubImpl(
        parent = parentStub,
        useSiteTargetRef = StringRef.fromString(useSiteTarget(tree, node).name)!!,
    )

    /** Mirrors `KtAnnotationUseSiteTarget.getAnnotationUseSiteTarget`: the first child token determines the target. */
    private fun useSiteTarget(tree: LighterAST, node: LighterASTNode): AnnotationUseSiteTarget {
        val firstChild = tree.getChildren(node).firstOrNull()
        return when (firstChild?.tokenType) {
            KtTokens.ALL_KEYWORD -> AnnotationUseSiteTarget.ALL
            KtTokens.FIELD_KEYWORD -> AnnotationUseSiteTarget.FIELD
            KtTokens.FILE_KEYWORD -> AnnotationUseSiteTarget.FILE
            KtTokens.PROPERTY_KEYWORD -> AnnotationUseSiteTarget.PROPERTY
            KtTokens.GET_KEYWORD -> AnnotationUseSiteTarget.PROPERTY_GETTER
            KtTokens.SET_KEYWORD -> AnnotationUseSiteTarget.PROPERTY_SETTER
            KtTokens.RECEIVER_KEYWORD -> AnnotationUseSiteTarget.RECEIVER
            KtTokens.PARAM_KEYWORD -> AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
            KtTokens.SETPARAM_KEYWORD -> AnnotationUseSiteTarget.SETTER_PARAMETER
            KtTokens.DELEGATE_KEYWORD -> AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
            else -> throw IllegalStateException("Unknown annotation target " + firstChild?.let(tree::nodeText))
        }
    }

    override fun serialize(stub: KotlinAnnotationUseSiteTargetStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.useSiteTarget)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinAnnotationUseSiteTargetStubImpl = KotlinAnnotationUseSiteTargetStubImpl(
        parent = parentStub,
        useSiteTargetRef = dataStream.readName()!!,
    )
}
