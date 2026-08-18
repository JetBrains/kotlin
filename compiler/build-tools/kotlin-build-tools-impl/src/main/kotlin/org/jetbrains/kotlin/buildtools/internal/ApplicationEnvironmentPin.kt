/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.incremental.clearJarCaches

internal object ApplicationEnvironmentPinProvider {
    fun create(): AutoCloseable = ApplicationEnvironmentPin()
}

private class ApplicationEnvironmentPin : AutoCloseable {
    private val disposable: Disposable = Disposer.newDisposable("kotlin-dsl BTA application environment pin")

    init {
        setupIdeaStandaloneExecution()
        @OptIn(CoreEnvironmentDeprecation::class)
        KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForProduction(
            disposable, CompilerConfiguration.create()
        )
    }

    override fun close() {
        clearJarCaches()
        Disposer.dispose(disposable)
    }
}