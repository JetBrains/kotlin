/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.utils

import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

@Suppress("SpellCheckingInspection")
internal object PublicApproximatorConfiguration : TypeApproximatorConfiguration.AllFlexibleSameValue() {
    override val allFlexible: Boolean get() = false
    override val errorType: Boolean get() = true
    override val definitelyNotNullType: Boolean get() = false
    override val integerLiteralConstantType: Boolean get() = true
    override val intersectionTypesInContravariantPositions: Boolean get() = true
    override val localTypes: Boolean get() = true
}
