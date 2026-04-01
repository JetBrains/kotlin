/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker

@Suppress("JUnitTestCaseWithNoTests")
class WasmMockedKlibLoaderTest : AbstractMockedKlibLoaderTest(KOTLIN_WASM_STDLIB_NAME, BuiltInsPlatform.WASM) {
    override val ownPlatformCheckers: List<KlibPlatformChecker>
        get() = listOf(
            KlibPlatformChecker.Wasm()
        )

    override val alienPlatformCheckers: List<KlibPlatformChecker>
        get() = listOf(
            KlibPlatformChecker.JS,
            KlibPlatformChecker.Native(),
            KlibPlatformChecker.Native("ios_arm64"),
            KlibPlatformChecker.NativeMetadata("ios_arm64"),
        )
}
