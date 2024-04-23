/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.components.KtSubtypingComponent
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.assertIsValidAndAccessible
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.types.AbstractTypeChecker

internal class KtFirSubtypingComponent(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : KtSubtypingComponent(), KtFirAnalysisSessionComponent {
    override fun isEqualTo(first: KtType, second: KtType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
        second.assertIsValidAndAccessible()
        check(first is KtFirType)
        check(second is KtFirType)
        return AbstractTypeChecker.equalTypes(
            createTypeCheckerContext(errorTypePolicy),
            first.coneType,
            second.coneType,
        )
    }

    override fun isSubTypeOf(subType: KtType, superType: KtType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
        superType.assertIsValidAndAccessible()
        check(subType is KtFirType)
        check(superType is KtFirType)
        return AbstractTypeChecker.isSubtypeOf(
            createTypeCheckerContext(errorTypePolicy),
            subType.coneType,
            superType.coneType,
        )
    }
}
