/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.name.StandardClassIds

public fun interface KtSuperTypesFilter {
    public fun filter(analysisSession: KtAnalysisSession, superType: KtType, symbol: KtClassOrObjectSymbol): Boolean

    public object NO_DEFAULT_TYPES : KtSuperTypesFilter {
        override fun filter(analysisSession: KtAnalysisSession, superType: KtType, symbol: KtClassOrObjectSymbol): Boolean {
            with(analysisSession) {
                if (superType.isAny) {
                    return false
                }

                if (symbol.classKind == KtClassKind.ANNOTATION_CLASS && superType.isClassTypeWithClassId(StandardClassIds.Annotation)) {
                    return false
                }

                if (symbol.classKind == KtClassKind.ENUM_CLASS && superType.isClassTypeWithClassId(StandardClassIds.Enum)) {
                    return false
                }

                return true
            }
        }
    }

    public object NO_ANY_FOR_INTERFACES : KtSuperTypesFilter {
        override fun filter(analysisSession: KtAnalysisSession, superType: KtType, symbol: KtClassOrObjectSymbol): Boolean {
            with(analysisSession) {
                return when (symbol.classKind) {
                    KtClassKind.INTERFACE -> !superType.isAny
                    else -> true
                }
            }
        }
    }

    public object ALL : KtSuperTypesFilter {
        override fun filter(analysisSession: KtAnalysisSession, superType: KtType, symbol: KtClassOrObjectSymbol): Boolean {
            return true
        }
    }

    public object NONE : KtSuperTypesFilter {
        override fun filter(analysisSession: KtAnalysisSession, superType: KtType, symbol: KtClassOrObjectSymbol): Boolean {
            return false
        }
    }

    public companion object {
        public operator fun invoke(predicate: KtAnalysisSession.(type: KtType, symbol: KtClassOrObjectSymbol) -> Boolean): KtSuperTypesFilter {
            return KtSuperTypesFilter { analysisSession, superType, symbol -> predicate(analysisSession, superType, symbol) }
        }
    }
}
