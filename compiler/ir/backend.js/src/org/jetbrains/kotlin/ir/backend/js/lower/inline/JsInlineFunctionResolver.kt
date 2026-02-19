/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolverReplacingCoroutineIntrinsics
import org.jetbrains.kotlin.ir.inline.InlineMode

internal class JsInlineFunctionResolver(
    context: LoweringContext,
    inlineMode: InlineMode,
) : InlineFunctionResolverReplacingCoroutineIntrinsics<LoweringContext>(context, inlineMode)
