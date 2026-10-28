/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.classFiles

/**
 * Marks APIs that read classes through IntelliJ `VirtualFile`s. The compiler reads classes through [BinaryClassFileHandle]s;
 * these APIs remain for the PSI Java view, the Analysis API and the backend, until they move to handles (KT-90203).
 */
@RequiresOptIn(
    message = "Reads classes through IntelliJ Platform VirtualFiles; prefer BinaryClassFileHandle",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR)
annotation class VirtualFileBasedApi
