/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object OptInNames {
    val OLD_EXPERIMENTAL_FQ_NAME = FqName("kotlin.Experimental")
    val OLD_USE_EXPERIMENTAL_FQ_NAME = FqName("kotlin.UseExperimental")
    val REQUIRES_OPT_IN_FQ_NAME = FqName("kotlin.RequiresOptIn")
    val REQUIRES_OPT_IN_CLASS_ID = ClassId.topLevel(REQUIRES_OPT_IN_FQ_NAME)
    val OPT_IN_FQ_NAME = FqName("kotlin.OptIn")
    val OPT_IN_CLASS_ID = ClassId.topLevel(OPT_IN_FQ_NAME)

    val WAS_EXPERIMENTAL_FQ_NAME = FqName("kotlin.WasExperimental")
    val WAS_EXPERIMENTAL_CLASS_ID = ClassId.topLevel(WAS_EXPERIMENTAL_FQ_NAME)
    val USE_EXPERIMENTAL_ANNOTATION_CLASS = Name.identifier("markerClass")
    val WAS_EXPERIMENTAL_ANNOTATION_CLASS = Name.identifier("markerClass")

    val EXPERIMENTAL_FQ_NAMES = setOf(OLD_EXPERIMENTAL_FQ_NAME, REQUIRES_OPT_IN_FQ_NAME)
    val USE_EXPERIMENTAL_FQ_NAMES = setOf(OLD_USE_EXPERIMENTAL_FQ_NAME, OPT_IN_FQ_NAME)
}