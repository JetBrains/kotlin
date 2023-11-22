/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.utils

const val SCRIPT_SPECIAL_NAME_STRING = "<script>"
const val SCRIPT_RECEIVER_NAME_PREFIX = "\$script_receiver"

enum class FirScriptCustomizationKind {
    DEFAULT,
    RESULT_PROPERTY,
    PARAMETER
}