/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.commonklib

internal enum class CommonKlibArgumentOperationKind(val displayName: String) {
    JS_KLIB("JS KLIB"),
    WASM_KLIB("Wasm KLIB"),
    JS_LINKING("JS linking"),
    WASM_LINKING("Wasm linking"),
}
