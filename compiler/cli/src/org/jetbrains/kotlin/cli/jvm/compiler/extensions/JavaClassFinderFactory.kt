/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.extensions

import org.jetbrains.kotlin.extensions.ExtensionPointDescriptor
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.java.JavaAnnotationProvider
import org.jetbrains.kotlin.load.java.JavaClassFinder
import java.io.File

interface JavaClassFinderFactory {

    companion object : ExtensionPointDescriptor<JavaClassFinderFactory>(
        "org.jetbrains.kotlin.javaClassFinderFactory",
        JavaClassFinderFactory::class.java
    )

    /**
     * Creates a JavaClassFinder for the given scope.
     *
     * @param scope The search scope for finding Java classes
     * @param annotationProvider Provider for Java annotations
     * @param findLocalFile Function to resolve file paths to File objects
     * @param defaultFinderProvider Optional provider for the platform's default JavaClassFinder.
     *        Can be used to create a hybrid finder that combines custom source-based lookup
     *        with the platform's binary class lookup. Returns null if no default is available.
     */
    fun createJavaClassFinder(
        scope: AbstractProjectFileSearchScope,
        annotationProvider: JavaAnnotationProvider?,
        findLocalFile: (String) -> File?,
        defaultFinderProvider: (() -> JavaClassFinder)? = null,
    ): JavaClassFinder
}