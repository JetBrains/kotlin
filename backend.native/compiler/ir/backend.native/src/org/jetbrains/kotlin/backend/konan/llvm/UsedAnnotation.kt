package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.name.FqName

private val annotationName = FqName("konan.Used")

internal val FunctionDescriptor.usedAnnotation: Boolean 
    get() {
        return (this.annotations.findAnnotation(annotationName) != null)
    }
