/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.*
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import java.io.IOException

open class StubIndexService protected constructor() {
    open fun indexFile(stub: KotlinFileStub, sink: IndexSink) {
    }

    open fun indexClass(stub: KotlinClassStub, sink: IndexSink) {
    }

    open fun indexFunction(stub: KotlinFunctionStub, sink: IndexSink) {
    }

    open fun indexTypeAlias(stub: KotlinTypeAliasStub, sink: IndexSink) {
    }

    open fun indexObject(stub: KotlinObjectStub, sink: IndexSink) {
    }

    open fun indexProperty(stub: KotlinPropertyStub, sink: IndexSink) {
    }

    open fun indexParameter(stub: KotlinParameterStub, sink: IndexSink) {
    }

    open fun indexAnnotation(stub: KotlinAnnotationEntryStub, sink: IndexSink) {
    }

    open fun indexScript(stub: KotlinScriptStub, sink: IndexSink) {
    }

    open fun createFileStub(file: KtFile): KotlinFileStub {
        return KotlinFileStubImpl(file, file.packageFqName.asString(), file.isScript())
    }

    @Throws(IOException::class)
    open fun serializeFileStub(stub: KotlinFileStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getPackageFqName().asString())
        dataStream.writeBoolean(stub.isScript())
    }

    @Throws(IOException::class)
    open fun deserializeFileStub(dataStream: StubInputStream): KotlinFileStub {
        val packageFqNameAsString = dataStream.readName()
        val isScript = dataStream.readBoolean()
        return KotlinFileStubImpl(null, packageFqNameAsString!!.string, isScript)
    }

    companion object {
        @JvmStatic
        fun getInstance(): StubIndexService {
            return ApplicationManager.getApplication().getService(StubIndexService::class.java) ?: NO_INDEX
        }

        private val NO_INDEX = StubIndexService()
    }
}
