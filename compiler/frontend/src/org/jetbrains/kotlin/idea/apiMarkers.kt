/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

/**
 * Indicates sensitive frontend API, which should be used with caution to avoid invariant violation.
 * Use sites of this annotation include all methods for direct access to frontend components.
 * Please make sure that components don't receive resolution results (descriptors etc.) from different resolution facade for processing.
 * The simplest way to do so is to explicitly provide the same resolution facade to all related computations.
 * Not following this rule may lead to obscure memory leaks and other potential problems.
 */
@RequiresOptIn
annotation class FrontendInternals
