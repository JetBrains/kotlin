/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.ir.backend.web.lower.serialization.ir.WebManglerIr

// Used in compose-ide-plugin
@Deprecated(
    message = "This object has been moved to org.jetbrains.kotlin.ir.backend.web.lower.serialization.ir.WebManglerIr",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("WebManglerIr", "org.jetbrains.kotlin.ir.backend.web.lower.serialization.ir.WebManglerIr")
)
typealias JsManglerIr = WebManglerIr