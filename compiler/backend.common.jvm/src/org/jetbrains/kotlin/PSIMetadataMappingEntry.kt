/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

class PSIMetadataMappingEntry(
    val name: String,
    val signature: String,
    val file: String,
    val packageName: String,
    val startOffset: Int,
    val endOffset: Int,
)