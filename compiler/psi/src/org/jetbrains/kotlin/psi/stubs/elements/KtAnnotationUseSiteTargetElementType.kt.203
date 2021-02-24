/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtAnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationUseSiteTargetStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinAnnotationUseSiteTargetStubImpl

class KtAnnotationUseSiteTargetElementType(debugName: String) :
    KtStubElementType<KotlinAnnotationUseSiteTargetStub, KtAnnotationUseSiteTarget>(
        debugName, KtAnnotationUseSiteTarget::class.java, KotlinAnnotationUseSiteTargetStub::class.java
    ) {

    override fun createStub(psi: KtAnnotationUseSiteTarget, parentStub: StubElement<out PsiElement>): KotlinAnnotationUseSiteTargetStub {
        val useSiteTarget = psi.getAnnotationUseSiteTarget().name
        return KotlinAnnotationUseSiteTargetStubImpl(parentStub, StringRef.fromString(useSiteTarget)!!)
    }

    override fun serialize(stub: KotlinAnnotationUseSiteTargetStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getUseSiteTarget())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<PsiElement>): KotlinAnnotationUseSiteTargetStub {
        val useSiteTarget = dataStream.readName()
        return KotlinAnnotationUseSiteTargetStubImpl(parentStub, useSiteTarget!!)
    }
}
