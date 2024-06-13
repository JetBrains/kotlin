/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.types.checker.IsErrorTypeEqualToAnythingTypeChecker
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.NewKotlinTypeCheckerImpl

internal class KaFe10TypeRelationChecker(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaSessionComponent<KaFe10Session>(), KaTypeRelationChecker, KaFe10SessionComponent {
    override fun KaType.semanticallyEquals(other: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean = withValidityAssertion {
        require(this is KaFe10Type)
        require(other is KaFe10Type)
        return getTypeCheckerFor(errorTypePolicy).equalTypes(this.fe10Type, other.fe10Type)
    }

    override fun KaType.isSubtypeOf(supertype: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean = withValidityAssertion {
        require(this is KaFe10Type)
        require(supertype is KaFe10Type)
        return getTypeCheckerFor(errorTypePolicy).isSubtypeOf(this.fe10Type, supertype.fe10Type)
    }

    private fun getTypeCheckerFor(errorTypePolicy: KaSubtypingErrorTypePolicy): KotlinTypeChecker {
        val typeChecker = analysisContext.resolveSession.kotlinTypeCheckerOfOwnerModule
        if (typeChecker !is NewKotlinTypeCheckerImpl) return typeChecker

        // `NewKotlinTypeCheckerImpl` is inconsistent with its error type leniency: `isSubtypeOf` is lenient by default while `equalTypes`
        // isn't. Hence, even without a `LENIENT` policy, we need to wrap `typeChecker` to achieve consistent strictness.
        return IsErrorTypeEqualToAnythingTypeChecker(typeChecker, errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT)
    }
}
