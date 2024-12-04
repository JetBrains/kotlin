/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

/**
 * Functions and classes annotated with [KtPsiInconsistencyHandling] are not intended for general-purpose use, but for working with possibly
 * inconsistent PSI. The specific circumstances need to be described in the documentation of the annotated function/class.
 *
 * Inconsistent PSI cannot be produced by the Kotlin parser. It occurs rarely, for example during modification of the PSI by the IDE. In
 * general, it can be assumed that all PSI is consistent. Inconsistent PSI should only be assumed when there is sufficient proof.
 */
@RequiresOptIn
annotation class KtPsiInconsistencyHandling
