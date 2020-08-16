/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.kdoc

import org.jetbrains.kotlin.builtins.KotlinBuiltInsNames.FqNames
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils

private val boringBuiltinClasses = setOf(
    FqNames.unit,
    FqNames._byte,
    FqNames._short,
    FqNames._int,
    FqNames._long,
    FqNames._char,
    FqNames._boolean,
    FqNames._float,
    FqNames._double,
    FqNames.string,
    FqNames.array,
    FqNames.any
)

fun ClassifierDescriptor.isBoringBuiltinClass(): Boolean = DescriptorUtils.getFqName(this) in boringBuiltinClasses
