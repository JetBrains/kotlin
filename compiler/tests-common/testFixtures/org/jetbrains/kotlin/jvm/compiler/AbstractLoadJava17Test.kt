/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

abstract class AbstractLoadJava17Test : AbstractLoadJavaTest() {
    override fun getJdkKind(): TestJdkKind = TestJdkKind.FULL_JDK_17
    override fun getJdkHomeForJavac(): File = KtTestUtil.getJdk17Home()
    override fun getAdditionalJavacArgs(): List<String> = ADDITIONAL_JAVAC_ARGS_FOR_17
}

val ADDITIONAL_JAVAC_ARGS_FOR_17  = listOf("--release", "17")
