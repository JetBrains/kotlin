// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.analysis.api.dumdum.stubindex

import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.kotlin.psi.KtCallableDeclaration

class KotlinTopLevelExtensionsByReceiverTypeIndex internal constructor() {
    companion object Helper : KotlinExtensionsByReceiverTypeStubIndexHelper() {
        override val indexKey: StubIndexKey<String, KtCallableDeclaration> =
            StubIndexKey.createIndexKey(KotlinTopLevelExtensionsByReceiverTypeIndex::class.java.simpleName)
    }
}