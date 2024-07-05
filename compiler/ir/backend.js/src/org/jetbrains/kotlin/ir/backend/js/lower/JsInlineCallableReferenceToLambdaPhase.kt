/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.inline.JsInlineFunctionResolver
import org.jetbrains.kotlin.ir.inline.CommonInlineCallableReferenceToLambdaPhase

internal class JsInlineCallableReferenceToLambdaPhase(context: JsIrBackendContext) : CommonInlineCallableReferenceToLambdaPhase(
    context, JsInlineFunctionResolver(context, inlineOnlyPrivateFunctions = false)
)
