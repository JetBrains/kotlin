/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.declarations.IrFunction

class JvmEvaluatorData(
    // This is populated by LocalDeclarationsLowering with the intermediate data allowing mapping from local function captures to parameters
    // and accurate transformation of calls to local functions from code fragments.
    val localDeclarationsLoweringData: MutableMap<IrFunction, JvmBackendContext.LocalFunctionData>
)
