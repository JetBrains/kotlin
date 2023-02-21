/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

// NOTE: The names of these annotations are chosen in such a way that IDEA highlights the calls of annotated functions
// with different colors (IDEA uses a hash of the annotation's name to determine the color of a function call).
// Please don't rename these annotations, or if you do, make sure that the annotation functions are highlighted with different colors.

@DslMarker
annotation class PrettyIrDsl

@DslMarker
@Target(AnnotationTarget.FUNCTION)
annotation class IrNodeBuilderDsl

@DslMarker
@Target(AnnotationTarget.FUNCTION)
annotation class IrNodePropertyDsl
