/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.JetAnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationUseSiteTargetStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinAnnotationUseSiteTargetStubImpl

public class JetAnnotationUseSiteTargetElementType(debugName: String) : JetStubElementType<KotlinAnnotationUseSiteTargetStub, JetAnnotationUseSiteTarget>(
        debugName, javaClass<JetAnnotationUseSiteTarget>(), javaClass<KotlinAnnotationUseSiteTargetStub>()
) {

    override fun createStub(psi: JetAnnotationUseSiteTarget, parentStub: StubElement<PsiElement>): KotlinAnnotationUseSiteTargetStub {
        val useSiteTarget = psi.getAnnotationUseSiteTarget().name()
        return KotlinAnnotationUseSiteTargetStubImpl(parentStub, StringRef.fromString(useSiteTarget)!!)
    }

    override fun serialize(stub: KotlinAnnotationUseSiteTargetStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getUseSiteTarget())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<PsiElement>): KotlinAnnotationUseSiteTargetStub {
        val useSiteTarget = dataStream.readName()
        return KotlinAnnotationUseSiteTargetStubImpl(parentStub, useSiteTarget)
    }
}
