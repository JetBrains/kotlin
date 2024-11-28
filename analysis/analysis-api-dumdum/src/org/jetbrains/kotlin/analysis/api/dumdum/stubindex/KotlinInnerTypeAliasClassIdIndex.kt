// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.api.dumdum.stubindex

import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.kotlin.psi.KtTypeAlias

/**
 * Index of non-top-level type aliases (members of classes)
 * The key is stringified [org.jetbrains.kotlin.name.ClassId] by the rules described in [org.jetbrains.kotlin.name.ClassId.asString]:
 * packages are delimited by '/' and classes by '.', e.g. "kotlin/Map.Entry"
 */
class KotlinInnerTypeAliasClassIdIndex internal constructor() {
    companion object Helper : KotlinStringStubIndexHelper<KtTypeAlias>(KtTypeAlias::class.java) {
        override val indexKey: StubIndexKey<String, KtTypeAlias> =
            StubIndexKey.createIndexKey("org.jetbrains.kotlin.idea.stubindex.KotlinInnerTypeAliasClassIdIndex")
    }
}