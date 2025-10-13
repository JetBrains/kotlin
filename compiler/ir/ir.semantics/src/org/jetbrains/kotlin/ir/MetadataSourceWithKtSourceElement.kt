/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.ir.declarations.MetadataSource

interface MetadataSourceWithKtSourceElement : MetadataSource {
    val source: KtSourceElement? get() = null
}

val MetadataSource.source: KtSourceElement?
    get() = (this as? MetadataSourceWithKtSourceElement)?.source