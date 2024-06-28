/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.utils

import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

@Suppress("SpellCheckingInspection")
internal class PublicApproximatorConfiguration(override val approximateLocalTypes: Boolean) : TypeApproximatorConfiguration.AllFlexibleSameValue() {
    override val approximateAllFlexible: Boolean get() = true
    override val approximateErrorTypes: Boolean get() = false
    override val approximateDefinitelyNotNullTypes: Boolean get() = true
    override val approximateIntegerLiteralConstantTypes: Boolean get() = true
    override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
}
