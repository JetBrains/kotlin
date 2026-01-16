/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.extensions

import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.java.JavaAnnotationProvider
import org.jetbrains.kotlin.load.java.JavaClassFinder

interface JavaClassFinderFactory {

    companion object : ProjectExtensionDescriptor<JavaClassFinderFactory>(
        "org.jetbrains.kotlin.javaClassFinderFactory",
        JavaClassFinderFactory::class.java
    )

    fun createJavaClassFinder(
        scope: AbstractProjectFileSearchScope,
        annotationProvider: JavaAnnotationProvider?
    ): JavaClassFinder
}