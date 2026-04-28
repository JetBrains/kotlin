/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.extensions

import com.intellij.openapi.vfs.VirtualFileSystem
import org.jetbrains.kotlin.extensions.ExtensionPointDescriptor
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.java.JavaAnnotationProvider
import org.jetbrains.kotlin.load.java.JavaClassFinder

interface JavaClassFinderFactory {

    companion object : ExtensionPointDescriptor<JavaClassFinderFactory>(
        "org.jetbrains.kotlin.javaClassFinderFactory",
        JavaClassFinderFactory::class.java
    )

    /**
     * Creates a JavaClassFinder for the given scope.
     *
     * @param scope The search scope for finding Java classes. Class-level scope; implementations
     *        that need to filter individual `.java` files for membership can use it via
     *        [scope].asPsiSearchScope().contains(file).
     * @param annotationProvider Provider for Java annotations
     * @param localFs The local [VirtualFileSystem] used by the project; implementations resolve
     *        paths via [VirtualFileSystem.findFileByPath] so reads benefit from the VFS caching layer.
     * @param defaultFinderProvider Optional provider for the platform's default JavaClassFinder.
     *        Can be used to create a hybrid finder that combines custom source-based lookup
     *        with the platform's binary class lookup. Returns null if no default is available.
     */
    fun createJavaClassFinder(
        scope: AbstractProjectFileSearchScope,
        annotationProvider: JavaAnnotationProvider?,
        localFs: VirtualFileSystem,
        defaultFinderProvider: (() -> JavaClassFinder)? = null,
    ): JavaClassFinder
}
