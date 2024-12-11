// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.analysis.api.dumdum.stubindex

import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.kotlin.psi.KtFile

class KotlinExactPackagesIndex internal constructor() {
    companion object Helper: KotlinStringStubIndexHelper<KtFile>(KtFile::class.java) {

        @JvmField
        val NAME: StubIndexKey<String, KtFile> = StubIndexKey.createIndexKey("org.jetbrains.kotlin.idea.stubindex.KotlinExactPackagesIndex")
        
        override val indexKey: StubIndexKey<String, KtFile>
            get() = NAME
    }
}