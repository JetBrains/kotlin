/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.utils

import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext

internal class PublicApproximatorConfiguration(
    override val approximateLocalTypes: Boolean,
    private val shouldApproximateLocalType: (TypeSystemInferenceExtensionContext, KotlinTypeMarker) -> Boolean = { _, _ -> true },
) : TypeApproximatorConfiguration() {
    override fun shouldApproximateLocalType(ctx: TypeSystemInferenceExtensionContext, type: KotlinTypeMarker): Boolean =
        shouldApproximateLocalType.invoke(ctx, type)

    override val approximateAllFlexible: Boolean get() = true
    override val approximateErrorTypes: Boolean get() = false
    override val approximateDefinitelyNotNullTypes: Boolean get() = true
    override val approximateIntegerLiteralConstantTypes: Boolean get() = true
    override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    override val approximateAnonymous: Boolean get() = true
}