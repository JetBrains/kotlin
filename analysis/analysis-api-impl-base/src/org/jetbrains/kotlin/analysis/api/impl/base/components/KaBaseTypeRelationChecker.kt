/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsTypeRelationChecker
import org.jetbrains.kotlin.analysis.api.lifetime.assertIsValidAndAccessible
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId

@KaImplementationDetail
abstract class KaBaseTypeRelationChecker<T : KaSession> : KaBaseSessionComponent<T>(), KaInternalsTypeRelationChecker {
    override fun isSubtypeOf(type: KaType, classId: ClassId, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
        if (type is KaErrorType) return errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT

        return type.isClassSubtypeOf(classId, errorTypePolicy)
    }

    protected abstract fun KaType.isClassSubtypeOf(classId: ClassId, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean

    override fun isSubtypeOf(type: KaType, symbol: KaClassLikeSymbol, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
        symbol.assertIsValidAndAccessible()

        if (type is KaErrorType) return errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT

        return type.isClassSubtypeOf(symbol, errorTypePolicy)
    }

    protected abstract fun KaType.isClassSubtypeOf(
        symbol: KaClassLikeSymbol,
        errorTypePolicy: KaSubtypingErrorTypePolicy,
    ): Boolean
}
