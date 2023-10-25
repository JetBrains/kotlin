/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.jvm

import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.descriptors.ClassKind

val BirClass.isJvmInterface: Boolean
    get() = kind == ClassKind.ANNOTATION_CLASS || kind == ClassKind.INTERFACE
