/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.assertIsValidAndAccessible
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.types.AbstractTypeChecker

internal class KaFirTypeRelationChecker(
    override val analysisSessionProvider: () -> KaFirSession
) : KaSessionComponent<KaFirSession>(), KaTypeRelationChecker, KaFirSessionComponent {
    override fun KaType.semanticallyEquals(other: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean = withValidityAssertion {
        other.assertIsValidAndAccessible()
        check(this is KaFirType)
        check(other is KaFirType)
        return AbstractTypeChecker.equalTypes(
            createTypeCheckerContext(errorTypePolicy),
            this.coneType,
            other.coneType,
        )
    }

    override fun KaType.isSubtypeOf(supertype: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean = withValidityAssertion {
        supertype.assertIsValidAndAccessible()
        check(this is KaFirType)
        check(supertype is KaFirType)
        return AbstractTypeChecker.isSubtypeOf(
            createTypeCheckerContext(errorTypePolicy),
            this.coneType,
            supertype.coneType,
        )
    }
}
