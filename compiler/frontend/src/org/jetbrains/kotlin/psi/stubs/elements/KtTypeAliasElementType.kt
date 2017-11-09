/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.stubs.KotlinTypeAliasStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeAliasStubImpl
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils

class KtTypeAliasElementType(debugName: String) :
        KtStubElementType<KotlinTypeAliasStub, KtTypeAlias>(debugName, KtTypeAlias::class.java, KotlinTypeAliasStub::class.java) {

    override fun createStub(psi: KtTypeAlias, parentStub: StubElement<*>?): KotlinTypeAliasStub {
        val name = StringRef.fromString(psi.name)
        val fqName = StringRef.fromString(ResolveSessionUtils.safeFqNameForLazyResolve(psi)?.asString())
        val isTopLevel = psi.isTopLevel()
        return KotlinTypeAliasStubImpl(parentStub, name, fqName, isTopLevel)
    }

    override fun serialize(stub: KotlinTypeAliasStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeName(stub.getFqName()?.asString())
        dataStream.writeBoolean(stub.isTopLevel())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinTypeAliasStub {
        val name = dataStream.readName()
        val fqName = dataStream.readName()
        val isTopLevel = dataStream.readBoolean()
        return KotlinTypeAliasStubImpl(parentStub, name, fqName, isTopLevel)
    }

    override fun indexStub(stub: KotlinTypeAliasStub, sink: IndexSink) {
        StubIndexService.getInstance().indexTypeAlias(stub, sink)
    }
}
