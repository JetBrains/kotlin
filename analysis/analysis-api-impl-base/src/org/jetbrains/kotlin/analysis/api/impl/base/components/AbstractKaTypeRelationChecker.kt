/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker
import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType

abstract class AbstractKaTypeRelationChecker<T : KaSession> : KaSessionComponent<T>(), KaTypeRelationChecker {
    override fun KaType.semanticallyEquals(other: KaType): Boolean = withValidityAssertion {
        return semanticallyEquals(other, KaSubtypingErrorTypePolicy.STRICT)
    }

    override fun KaType.isSubTypeOf(superType: KaType): Boolean = withValidityAssertion {
        return isSubTypeOf(superType, KaSubtypingErrorTypePolicy.STRICT)
    }

    override fun KaType.isNotSubTypeOf(superType: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean = withValidityAssertion {
        return !isSubTypeOf(superType, errorTypePolicy)
    }

    override fun KaType.isNotSubTypeOf(superType: KaType): Boolean = withValidityAssertion {
        return !isSubTypeOf(superType)
    }
}