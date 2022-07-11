/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

const val SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME = "\$completion"
const val SUSPEND_CALL_RESULT_NAME = "\$result"
const val ILLEGAL_STATE_ERROR_MESSAGE = "call to 'resume' before 'invoke' with coroutine"