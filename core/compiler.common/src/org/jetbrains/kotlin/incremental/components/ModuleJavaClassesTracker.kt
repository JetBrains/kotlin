/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.components

import org.jetbrains.kotlin.name.ClassId
import java.io.File

/**
 * ModuleJavaClassesTracker is used to track class ids of the java classes compiled along with Kotlin sources
 * The name is made specifically different from descriptors-based JavaClassesTracker
 */
interface ModuleJavaClassesTracker {

    /**
     * Report encounter of a class from the java source file
     */
    fun report(classId: ClassId, file: File?)
}
