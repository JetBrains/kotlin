/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.components.KtSubtypingComponent
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.types.checker.IsErrorTypeEqualToAnythingTypeChecker
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.NewKotlinTypeCheckerImpl

internal class KtFe10SubtypingComponent(
    override val analysisSession: KtFe10AnalysisSession
) : KtSubtypingComponent(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    override fun isEqualTo(first: KtType, second: KtType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
        require(first is KtFe10Type)
        require(second is KtFe10Type)
        return getTypeCheckerFor(errorTypePolicy).equalTypes(first.fe10Type, second.fe10Type)
    }

    override fun isSubTypeOf(subType: KtType, superType: KtType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
        require(subType is KtFe10Type)
        require(superType is KtFe10Type)
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
