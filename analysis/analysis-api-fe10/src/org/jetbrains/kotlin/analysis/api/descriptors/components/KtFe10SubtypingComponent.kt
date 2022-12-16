/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtSubtypingComponent
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtType

internal class KtFe10SubtypingComponent(
    override val analysisSession: KtFe10AnalysisSession
) : KtSubtypingComponent(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    override fun isEqualTo(first: KtType, second: KtType): Boolean {
        require(first is KtFe10Type)
        require(second is KtFe10Type)
        return analysisContext.resolveSession.kotlinTypeCheckerOfOwnerModule.equalTypes(first.fe10Type, second.fe10Type)
    }

    override fun isSubTypeOf(subType: KtType, superType: KtType): Boolean {
        require(subType is KtFe10Type)
        require(superType is KtFe10Type)
        val typeChecker = analysisContext.resolveSession.kotlinTypeCheckerOfOwnerModule
        return typeChecker.isSubtypeOf(subType.fe10Type, superType.fe10Type)
    }
}