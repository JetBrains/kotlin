/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

abstract class AbstractJdk15BlackBoxCodegenTest : AbstractCustomJDKBlackBoxCodegenTest() {
    override fun getTestJdkKind(): TestJdkKind = TestJdkKind.FULL_JDK_15
    override fun getJdkHome(): File = KotlinTestUtils.getJdk15Home()
    override fun getPrefix(): String = "java15/box"

    override fun getAdditionalJvmArgs(): List<String> = listOf("--enable-preview")

    override fun verifyWithDex(): Boolean = false
}
