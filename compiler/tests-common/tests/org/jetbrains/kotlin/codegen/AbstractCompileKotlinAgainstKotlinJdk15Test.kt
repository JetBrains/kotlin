/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind

abstract class AbstractCompileKotlinAgainstKotlinJdk15Test : AbstractCompileKotlinAgainstKotlinTest() {
    override fun invokeBox(className: String) {
        runJvmInstance(
            KotlinTestUtils.getJdk15Home(),
            additionalArgs = listOf("--enable-preview"),
            classPath = listOfNotNull(
                aDir, bDir, ForTestCompileRuntime.runtimeJarForTests(),
            ),
            className
        )
    }

    override fun getTestJdkKind(files: List<TestFile>): TestJdkKind {
        return TestJdkKind.FULL_JDK_15
    }
}
