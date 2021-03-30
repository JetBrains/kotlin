/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.load.java.sam.JavaSingleAbstractMethodUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.backend.common.SamTypeFactory

object JvmSamTypeFactory : SamTypeFactory() {
    override fun isSamType(type: KotlinType) = JavaSingleAbstractMethodUtils.isSamType(type)
}