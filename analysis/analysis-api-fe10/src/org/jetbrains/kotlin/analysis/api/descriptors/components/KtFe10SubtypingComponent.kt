/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.components.KaSubtypingComponent
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.types.checker.IsErrorTypeEqualToAnythingTypeChecker
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.NewKotlinTypeCheckerImpl

internal class KaFe10SubtypingComponent(
    override val analysisSession: KaFe10Session
) : KaSubtypingComponent(), KaFe10SessionComponent {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    override fun isEqualTo(first: KaType, second: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
        require(first is KaFe10Type)
        require(second is KaFe10Type)
        return getTypeCheckerFor(errorTypePolicy).equalTypes(first.fe10Type, second.fe10Type)
    }

    override fun isSubTypeOf(subType: KaType, superType: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
        require(subType is KaFe10Type)
        require(superType is KaFe10Type)
        return getTypeCheckerFor(errorTypePolicy).isSubtypeOf(subType.fe10Type, superType.fe10Type)
    }

    private fun getTypeCheckerFor(errorTypePolicy: KaSubtypingErrorTypePolicy): KotlinTypeChecker {
        val typeChecker = analysisContext.resolveSession.kotlinTypeCheckerOfOwnerModule
        if (typeChecker !is NewKotlinTypeCheckerImpl) return typeChecker

        // `NewKotlinTypeCheckerImpl` is inconsistent with its error type leniency: `isSubtypeOf` is lenient by default while `equalTypes`
        // isn't. Hence, even without a `LENIENT` policy, we need to wrap `typeChecker` to achieve consistent strictness.
        return IsErrorTypeEqualToAnythingTypeChecker(typeChecker, errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT)
    }
}
