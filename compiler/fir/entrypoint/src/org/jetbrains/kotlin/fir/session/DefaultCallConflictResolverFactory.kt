/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.calls.ConeCompositeConflictResolver
import org.jetbrains.kotlin.fir.resolve.calls.ConeIntegerOperatorConflictResolver
import org.jetbrains.kotlin.fir.resolve.calls.ConeOverloadConflictResolver
import org.jetbrains.kotlin.fir.resolve.calls.jvm.ConeEquivalentCallConflictResolver
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator

object DefaultCallConflictResolverFactory : ConeCallConflictResolverFactory() {
    override fun create(
        typeSpecificityComparator: TypeSpecificityComparator,
        components: InferenceComponents,
        transformerComponents: BodyResolveComponents
    ): ConeCompositeConflictResolver {
        val specificityComparator = TypeSpecificityComparator.NONE
        // NB: Adding new resolvers is strongly discouraged because the results are order-dependent.
        return ConeCompositeConflictResolver(
            ConeEquivalentCallConflictResolver(specificityComparator, components, transformerComponents),
            ConeIntegerOperatorConflictResolver,
            ConeOverloadConflictResolver(specificityComparator, components, transformerComponents),
        )
    }
}
