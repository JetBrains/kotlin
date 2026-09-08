/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtAnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinAnnotationUseSiteTargetStubImpl

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
