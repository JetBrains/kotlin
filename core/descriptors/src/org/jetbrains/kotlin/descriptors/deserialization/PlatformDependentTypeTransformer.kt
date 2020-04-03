/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.deserialization

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.SimpleType

interface PlatformDependentTypeTransformer {
    fun transformPlatformType(classId: ClassId, computedType: SimpleType): SimpleType

    object None : PlatformDependentTypeTransformer {
        override fun transformPlatformType(classId: ClassId, computedType: SimpleType): SimpleType = computedType
    }
}