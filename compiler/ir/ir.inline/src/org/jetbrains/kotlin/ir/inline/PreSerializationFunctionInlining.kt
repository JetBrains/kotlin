/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext

internal class PreSerializationPrivateFunctionInlining(context: PreSerializationLoweringContext) : FunctionInlining(
    context,
    PreSerializationPrivateInlineFunctionResolver(context)
)

internal class PreSerializationIntraModuleFunctionInlining(context: PreSerializationLoweringContext) : FunctionInlining(
    context,
    PreSerializationNonPrivateInlineFunctionResolver(context, inlineCrossModuleFunctions = true)
)

internal class PreSerializationAllFunctionInlining(context: PreSerializationLoweringContext) : FunctionInlining(
    context,
    PreSerializationNonPrivateInlineFunctionResolver(context, inlineCrossModuleFunctions = false)
)
