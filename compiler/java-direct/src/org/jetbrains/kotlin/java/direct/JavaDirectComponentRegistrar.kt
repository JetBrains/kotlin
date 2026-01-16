/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.cli.jvm.compiler.extensions.JavaClassFinderFactory
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.java.JavaAnnotationProvider
import org.jetbrains.kotlin.load.java.JavaClassFinder

class JavaDirectComponentRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        JavaClassFinderFactory.registerExtension(JavaClassFinderOverAstFactory(configuration))
    }

    override val pluginId: String get() = PLUGIN_ID

    override val supportsK2: Boolean
        get() = true
}

class JavaClassFinderOverAstFactory(private val configuration: CompilerConfiguration): JavaClassFinderFactory {
    override fun createJavaClassFinder(
        scope: AbstractProjectFileSearchScope,
        annotationProvider: JavaAnnotationProvider?,
    ): JavaClassFinder {
        TODO("Not yet implemented")
    }
}

private const val PLUGIN_ID = "org.jetbrains.kotlin.javaDirect"