/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.idea.frontend.api.assertIsValidAndAccessible
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtSubtypingComponent
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.types.KtFirType
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.types.AbstractTypeChecker

internal class KtFirSubtypingComponent(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtSubtypingComponent(), KtFirAnalysisSessionComponent {
    override fun isEqualTo(first: KtType, second: KtType): Boolean = withValidityAssertion {
        second.assertIsValidAndAccessible()
        check(first is KtFirType)
        check(second is KtFirType)
        return AbstractTypeChecker.equalTypes(
            createTypeCheckerContext(),
            first.coneType,
            second.coneType
        )
    }

    override fun isSubTypeOf(subType: KtType, superType: KtType): Boolean = withValidityAssertion {
        superType.assertIsValidAndAccessible()
        check(subType is KtFirType)
        check(superType is KtFirType)
        return AbstractTypeChecker.isSubtypeOf(
            createTypeCheckerContext(),
            subType.coneType,
            superType.coneType
        )
    }
}
