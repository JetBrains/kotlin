/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.components.KaSubtypingComponent
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.assertIsValidAndAccessible
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.types.AbstractTypeChecker

internal class KaFirSubtypingComponent(
    override val analysisSession: KaFirSession,
    override val token: KaLifetimeToken,
) : KaSubtypingComponent(), KaFirSessionComponent {
    override fun isEqualTo(first: KaType, second: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
        second.assertIsValidAndAccessible()
        check(first is KaFirType)
        check(second is KaFirType)
        return AbstractTypeChecker.equalTypes(
            createTypeCheckerContext(errorTypePolicy),
            first.coneType,
            second.coneType,
        )
    }

    override fun isSubTypeOf(subType: KaType, superType: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
        superType.assertIsValidAndAccessible()
        check(subType is KaFirType)
        check(superType is KaFirType)
        return AbstractTypeChecker.isSubtypeOf(
            createTypeCheckerContext(errorTypePolicy),
            subType.coneType,
            superType.coneType,
        )
    }
}
