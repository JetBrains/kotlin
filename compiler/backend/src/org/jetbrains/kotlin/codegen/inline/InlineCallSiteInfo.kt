/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

class InlineCallSiteInfo(
    val ownerClassName: String,
    val functionName: String?,
    val functionDesc: String?,
    val isInlineOrInsideInline: Boolean,
    val isSuspend: Boolean,
    val lineNumber: Int
)