/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.name.ClassId

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsTypeRelationChecker {
    public fun semanticallyEquals(type: KaType, other: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean

    public fun isSubtypeOf(type: KaType, supertype: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean

    public fun isSubtypeOf(type: KaType, classId: ClassId, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean

    public fun isSubtypeOf(type: KaType, symbol: KaClassLikeSymbol, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean
}
