/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

val ConeKotlinType.isNullable: Boolean get() = nullability != ConeNullability.NOT_NULL

val ConeKotlinType.isMarkedNullable: Boolean get() = nullability == ConeNullability.NULLABLE

val ConeKotlinType.classId: ClassId? get() = this.safeAs<ConeClassLikeType>()?.lookupTag?.classId