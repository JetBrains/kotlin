/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class KtFunctionLikeSymbol : KtCallableSymbol(), KtTypedSymbol, KtSymbolWithKind {
    abstract val valueParameters: List<KtParameterSymbol>

    abstract override fun createPointer(): KtSymbolPointer<KtFunctionLikeSymbol>
}

abstract class KtAnonymousFunctionSymbol : KtFunctionLikeSymbol(), KtPossibleExtensionSymbol {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.LOCAL

    abstract override fun createPointer(): KtSymbolPointer<KtAnonymousFunctionSymbol>
}

data class KtCallableId(
    val packageName: FqName,
    val className: FqName?,
    val callableName: Name
) {
    var classId: ClassId? = null
        get() {
            if (field == null && className != null) {
                field = ClassId(packageName, className, false)
            }
            return field
        }

    fun asFqNameForDebugInfo(): FqName {
        return classId?.asSingleFqName()?.child(callableName) ?: packageName.child(callableName)
    }
}

abstract class KtFunctionSymbol : KtFunctionLikeSymbol(),
    KtNamedSymbol,
    KtPossibleExtensionSymbol,
    KtPossibleMemberSymbol,
    KtSymbolWithTypeParameters,
    KtSymbolWithModality<KtCommonSymbolModality>,
    KtSymbolWithVisibility,
    KtAnnotatedSymbol {
    abstract val callableIdIfNonLocal: KtCallableId?

    abstract val isSuspend: Boolean
    abstract val isOperator: Boolean
    abstract val isExternal: Boolean
    abstract val isInline: Boolean
    abstract val isOverride: Boolean

    abstract override val valueParameters: List<KtFunctionParameterSymbol>

    abstract override fun createPointer(): KtSymbolPointer<KtFunctionSymbol>
}

abstract class KtConstructorSymbol : KtFunctionLikeSymbol(), KtPossibleMemberSymbol, KtAnnotatedSymbol, KtSymbolWithVisibility {
    abstract val isPrimary: Boolean
    abstract val containingClassIdIfNonLocal: ClassId?

    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER

    abstract override val valueParameters: List<KtConstructorParameterSymbol>

    abstract override fun createPointer(): KtSymbolPointer<KtConstructorSymbol>
}