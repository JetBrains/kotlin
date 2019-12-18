/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.jvm

import org.jetbrains.kotlin.fir.resolve.calls.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.calls.ConeCompositeConflictResolver
import org.jetbrains.kotlin.fir.resolve.calls.ConeOverloadConflictResolver
import org.jetbrains.kotlin.fir.resolve.calls.InferenceComponents
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator

object JvmCallConflictResolverFactory : ConeCallConflictResolverFactory() {
    override fun create(
        typeSpecificityComparator: TypeSpecificityComparator,
        components: InferenceComponents
    ) = ConeCompositeConflictResolver(
        ConeOverloadConflictResolver(TypeSpecificityComparator.NONE, components),
        ConeEquivalentCallConflictResolver(TypeSpecificityComparator.NONE, components)
    )

}