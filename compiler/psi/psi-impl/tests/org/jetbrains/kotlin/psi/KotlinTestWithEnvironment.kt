/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.disposeRootDisposable
import org.junit.jupiter.api.AfterEach

abstract class KotlinTestWithEnvironment {
    private val testRootDisposable = Disposer.newDisposable()
    private val environment: KotlinCoreEnvironment = createEnvironment()
    val project: Project get() = environment.project

    @AfterEach
    fun tearDown() {
        disposeRootDisposable(testRootDisposable)
    }

    private fun createEnvironment(): KotlinCoreEnvironment {
        @OptIn(CoreEnvironmentDeprecation::class)
        return KotlinCoreEnvironment.createForParallelTests(
            testRootDisposable, KotlinTestUtils.newConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }
}
