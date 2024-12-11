// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.analysis.api.dumdum.stubindex

import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.kotlin.psi.KtAnnotationEntry

class KotlinAnnotationsIndex internal constructor() {
    companion object Helper : KotlinStringStubIndexHelper<KtAnnotationEntry>(KtAnnotationEntry::class.java) {
        override val indexKey: StubIndexKey<String, KtAnnotationEntry> =
            StubIndexKey.createIndexKey("org.jetbrains.kotlin.idea.stubindex.KotlinAnnotationsIndex")
    }
}