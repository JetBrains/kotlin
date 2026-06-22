/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtAnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinAnnotationUseSiteTargetStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinAnnotationUseSiteTargetStubFactory :
    StubElementFactory<KotlinAnnotationUseSiteTargetStubImpl, KtAnnotationUseSiteTarget> {
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(
        psi: KtAnnotationUseSiteTarget,
        parentStub: StubElement<out PsiElement>?,
    ): KotlinAnnotationUseSiteTargetStubImpl {
        val useSiteTarget = psi.getAnnotationUseSiteTarget().name
        return KotlinAnnotationUseSiteTargetStubImpl(parentStub, StringRef.fromString(useSiteTarget)!!)
    }

    override fun createPsi(stub: KotlinAnnotationUseSiteTargetStubImpl): KtAnnotationUseSiteTarget =
        KtAnnotationUseSiteTarget(stub)
}

internal object KotlinAnnotationUseSiteTargetStubSerializer : StubSerializer<KotlinAnnotationUseSiteTargetStubImpl> {
    override fun getExternalId(): String = "kotlin.ANNOTATION_TARGET"

    override fun serialize(stub: KotlinAnnotationUseSiteTargetStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.useSiteTarget)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinAnnotationUseSiteTargetStubImpl {
        val useSiteTarget = dataStream.readName()
        return KotlinAnnotationUseSiteTargetStubImpl(parentStub, useSiteTarget!!)
    }

    override fun indexStub(stub: KotlinAnnotationUseSiteTargetStubImpl, sink: IndexSink) {
        // not indexed
    }
}
