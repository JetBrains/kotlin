/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.name.ClassId

interface JavaModuleAnnotationsProvider {
    fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>?
}
