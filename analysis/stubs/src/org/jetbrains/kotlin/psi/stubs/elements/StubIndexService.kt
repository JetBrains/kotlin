/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.openapi.components.serviceOrNull
import com.intellij.psi.stubs.IndexSink
import org.jetbrains.kotlin.psi.KtIdeApi
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.*

@KtIdeApi
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

    companion object {
        @KtImplementationDetail
        fun getInstance(): StubIndexService? = serviceOrNull()
    }
}
