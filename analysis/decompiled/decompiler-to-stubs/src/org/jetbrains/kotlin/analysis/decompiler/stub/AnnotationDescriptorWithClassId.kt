/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.KotlinType

abstract class AnnotationDescriptorWithClassId(val classId: ClassId) : AnnotationDescriptor {
    override val type: KotlinType
        get() {
            error("Should not be called")
        }
    override val fqName: FqName?
        get() = classId.asSingleFqName()
}