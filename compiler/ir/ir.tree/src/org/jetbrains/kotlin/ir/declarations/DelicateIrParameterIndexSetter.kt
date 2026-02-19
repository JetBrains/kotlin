/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

@RequiresOptIn(
    "Index of a parameter is tracked automatically when adding/removing it to/from IrFunction. " +
            "Only a few selected places should need to modify it manually. " +
            "One example is IrScript, whose parameters have non-obvious and not-automatic indices.",
    level = RequiresOptIn.Level.ERROR
)
@Target(AnnotationTarget.PROPERTY_SETTER)
annotation class DelicateIrParameterIndexSetter