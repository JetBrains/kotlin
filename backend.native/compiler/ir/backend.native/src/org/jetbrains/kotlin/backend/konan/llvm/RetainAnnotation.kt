/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.name.FqName

private val annotationName = FqName("kotlin.native.Retain")

internal val FunctionDescriptor.retainAnnotation: Boolean
    get() {
        return (this.annotations.findAnnotation(annotationName) != null)
    }
