/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.kdoc

import org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils

private val boringBuiltinClasses = setOf(
    FQ_NAMES.unit,
    FQ_NAMES._byte,
    FQ_NAMES._short,
    FQ_NAMES._int,
    FQ_NAMES._long,
    FQ_NAMES._char,
    FQ_NAMES._boolean,
    FQ_NAMES._float,
    FQ_NAMES._double,
    FQ_NAMES.string,
    FQ_NAMES.array,
    FQ_NAMES.any
)

fun ClassifierDescriptor.isBoringBuiltinClass(): Boolean = DescriptorUtils.getFqName(this) in boringBuiltinClasses