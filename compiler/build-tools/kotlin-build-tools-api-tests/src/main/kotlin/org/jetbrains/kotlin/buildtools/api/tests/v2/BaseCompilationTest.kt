/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.v2

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.buildtools.api.tests.BaseTest
import org.jetbrains.kotlin.buildtools.api.tests.v2.BaseTestV2
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@TestDataPath("\$CONTENT_ROOT/../main/resources/modules")
abstract class BaseCompilationTest : BaseTestV2() {
    @TempDir
    lateinit var workingDirectory: Path
}