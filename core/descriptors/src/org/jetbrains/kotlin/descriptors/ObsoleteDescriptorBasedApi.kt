/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

/**
 * USE JUST FOR EXPERIMENTS.
 * Exact copy of [ObsoleteDescriptorBasedAPI]  since the original is not avaible in the scope of the experiments due to circular dependency.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS
)
@RequiresOptIn(message = "Please use IR declaration properties and not its descriptor properties", level = RequiresOptIn.Level.ERROR)
annotation class ObsoleteDescriptorBasedApi
