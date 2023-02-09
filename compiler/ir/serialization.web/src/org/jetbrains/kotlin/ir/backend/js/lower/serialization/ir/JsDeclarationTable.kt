/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.ir.backend.web.lower.serialization.ir.WebGlobalDeclarationTable

// Used in compose-ide-plugin
@Deprecated(
    message = "This class has been moved to org.jetbrains.kotlin.ir.backend.web.lower.serialization.ir.WebGlobalDeclarationTable",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith(
        "WebGlobalDeclarationTable",
        "org.jetbrains.kotlin.ir.backend.web.lower.serialization.ir.WebGlobalDeclarationTable"
    )
)
typealias JsGlobalDeclarationTable = WebGlobalDeclarationTable