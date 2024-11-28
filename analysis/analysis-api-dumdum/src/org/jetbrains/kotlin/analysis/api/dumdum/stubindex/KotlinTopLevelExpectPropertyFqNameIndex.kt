// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.analysis.api.dumdum.stubindex

import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Stores top level properties with `expect` modifier by full qualified name
 */
class KotlinTopLevelExpectPropertyFqNameIndex internal constructor() {
    companion object Helper : KotlinStringStubIndexHelper<KtProperty>(KtProperty::class.java) {
        override val indexKey: StubIndexKey<String, KtProperty> =
            StubIndexKey.createIndexKey(KotlinTopLevelExpectPropertyFqNameIndex::class.simpleName!!)
    }
}