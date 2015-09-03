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

import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.stubs.*
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl

public open class StubIndexService protected constructor() {
    public open fun indexFile(stub: KotlinFileStub, sink: IndexSink) {
    }

    public open fun indexClass(stub: KotlinClassStub, sink: IndexSink) {
    }

    public open fun indexFunction(stub: KotlinFunctionStub, sink: IndexSink) {
    }

    public open fun indexObject(stub: KotlinObjectStub, sink: IndexSink) {
    }

    public open fun indexProperty(stub: KotlinPropertyStub, sink: IndexSink) {
    }

    public open fun indexAnnotation(stub: KotlinAnnotationEntryStub, sink: IndexSink) {
    }

    public open fun createFileStub(file: JetFile): KotlinFileStub {
        return KotlinFileStubImpl(file, file.packageFqNameByTree.asString(), file.isScriptByTree)
    }

    companion object {
        @jvmStatic
        public fun getInstance(): StubIndexService {
            return ServiceManager.getService(StubIndexService::class.java) ?: NO_INDEX
        }

        private val NO_INDEX = StubIndexService()
    }
}
