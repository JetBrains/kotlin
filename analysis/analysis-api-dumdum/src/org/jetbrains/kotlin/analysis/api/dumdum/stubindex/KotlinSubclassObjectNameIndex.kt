// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.analysis.api.dumdum.stubindex

import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.kotlin.psi.KtObjectDeclaration

/**
 * An index to enable faster search of named objects with non-empty superclass lists.
 *
 * The main purpose of the index is to enable automatic import and code completion
 * for functions and properties declared in classed but importable from their child objects.
 *
 * See [org.jetbrains.kotlin.idea.core.KotlinIndicesHelper.processAllCallablesFromSubclassObjects]
 * for a usage example.
 */
class KotlinSubclassObjectNameIndex internal constructor() {
    companion object Helper : KotlinStringStubIndexHelper<KtObjectDeclaration>(KtObjectDeclaration::class.java) {
        override val indexKey: StubIndexKey<String, KtObjectDeclaration> =
            StubIndexKey.createIndexKey("org.jetbrains.kotlin.idea.stubindex.KotlinSubclassObjectNameIndex")
    }
}
