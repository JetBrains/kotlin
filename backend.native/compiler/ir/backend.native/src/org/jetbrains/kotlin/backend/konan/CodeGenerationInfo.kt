/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName

internal fun IrClass.isNonGeneratedAnnotation(): Boolean =
        this.kind == ClassKind.ANNOTATION_CLASS &&
                !this.annotations.hasAnnotation(serialInfoAnnotationFqName)

private val serialInfoAnnotationFqName = FqName("kotlinx.serialization.SerialInfo")
