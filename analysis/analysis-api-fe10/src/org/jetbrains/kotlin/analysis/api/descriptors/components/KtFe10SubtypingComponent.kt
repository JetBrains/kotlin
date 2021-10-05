/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtSubtypingComponent
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion

internal class KtFe10SubtypingComponent(override val analysisSession: KtFe10AnalysisSession) : KtSubtypingComponent() {
    override val token: ValidityToken
        get() = analysisSession.token

    override fun isEqualTo(first: KtType, second: KtType): Boolean = withValidityAssertion {
        require(first is KtFe10Type)
        require(second is KtFe10Type)
        return analysisSession.resolveSession.kotlinTypeCheckerOfOwnerModule.equalTypes(first.type, second.type)
    }

    override fun isSubTypeOf(subType: KtType, superType: KtType): Boolean = withValidityAssertion {
        require(subType is KtFe10Type)
        require(superType is KtFe10Type)
        val typeChecker = analysisSession.resolveSession.kotlinTypeCheckerOfOwnerModule
        return typeChecker.isSubtypeOf(subType.type, superType.type)
    }
}