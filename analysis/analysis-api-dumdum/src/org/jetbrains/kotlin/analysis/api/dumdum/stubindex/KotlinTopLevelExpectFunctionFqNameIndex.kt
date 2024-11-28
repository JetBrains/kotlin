// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.analysis.api.dumdum.stubindex

import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Stores top level functions with `expect` modifier by full qualified name
 */
class KotlinTopLevelExpectFunctionFqNameIndex internal constructor() {
    companion object Helper : KotlinStringStubIndexHelper<KtNamedFunction>(KtNamedFunction::class.java) {
        override val indexKey: StubIndexKey<String, KtNamedFunction> =
            StubIndexKey.createIndexKey(KotlinTopLevelExpectFunctionFqNameIndex::class.simpleName!!)
    }
}