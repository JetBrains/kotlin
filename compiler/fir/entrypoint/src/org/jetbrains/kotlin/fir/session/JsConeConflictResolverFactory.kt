/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.calls.ConeCompositeConflictResolver
import org.jetbrains.kotlin.fir.resolve.calls.ConeIntegerOperatorConflictResolver
import org.jetbrains.kotlin.fir.resolve.calls.ConeOverloadConflictResolver
import org.jetbrains.kotlin.fir.resolve.calls.jvm.ConeEquivalentCallConflictResolver
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.js.resolve.JsTypeSpecificityComparatorWithoutDelegate
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator

@NoMutableState
object JsCallConflictResolverFactory : ConeCallConflictResolverFactory() {
    override fun create(
        typeSpecificityComparator: TypeSpecificityComparator,
        components: InferenceComponents,
        transformerComponents: BodyResolveComponents
    ): ConeCompositeConflictResolver {
        val specificityComparator = JsTypeSpecificityComparatorWithoutDelegate(components.session.typeContext)
        // NB: Please, be aware that adding might not necessarily help you because ConeOverloadConflictResolver doesn't just filter out
        // less specific candidates, but leave the set the same if there are more than one same-specifity candidates.
        // Thus, in that case, your new ConeCallConflictResolver might get all the candidates in that case.
        return ConeCompositeConflictResolver(
            ConeOverloadConflictResolver(specificityComparator, components, transformerComponents),
            ConeEquivalentCallConflictResolver(specificityComparator, components, transformerComponents),
            ConeIntegerOperatorConflictResolver,
        )
    }
}
