/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub

import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name


data class AnnotationWithArgs(val classId: ClassId, val args: Map<Name, ConstantValue<*>>)
