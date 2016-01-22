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
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.stubs.KotlinScriptStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinScriptStubImpl

class KtScriptElementType(debugName: String) : KtStubElementType<KotlinScriptStub, KtScript>(
        debugName, KtScript::class.java, KotlinScriptStub::class.java
) {

    override fun createStub(psi: KtScript, parentStub: StubElement<PsiElement>): KotlinScriptStub {
        return KotlinScriptStubImpl(parentStub, StringRef.fromString(psi.fqName.asString()))
    }

    override fun serialize(stub: KotlinScriptStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getFqName().asString())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<PsiElement>): KotlinScriptStub {
        val fqName = dataStream.readName()
        return KotlinScriptStubImpl(parentStub, fqName)
    }


    override fun indexStub(stub: KotlinScriptStub, sink: IndexSink) {
        StubIndexService.getInstance().indexScript(stub, sink)
    }
}
