/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.StandardClassIds

@KaExperimentalApi
public fun interface KaSuperTypesFilter {
    public fun filter(analysisSession: KaSession, superType: KaType, symbol: KaClassOrObjectSymbol): Boolean

    public object NO_DEFAULT_TYPES : KaSuperTypesFilter {
        override fun filter(analysisSession: KaSession, superType: KaType, symbol: KaClassOrObjectSymbol): Boolean {
            with(analysisSession) {
                if (superType.isAnyType) {
                    return false
                }

                if (symbol.classKind == KaClassKind.ANNOTATION_CLASS && superType.isClassType(StandardClassIds.Annotation)) {
                    return false
                }

                if (symbol.classKind == KaClassKind.ENUM_CLASS && superType.isClassType(StandardClassIds.Enum)) {
                    return false
                }

                return true
            }
        }
    }

    public object NO_ANY_FOR_INTERFACES : KaSuperTypesFilter {
        override fun filter(analysisSession: KaSession, superType: KaType, symbol: KaClassOrObjectSymbol): Boolean {
            with(analysisSession) {
                return when (symbol.classKind) {
                    KaClassKind.INTERFACE -> !superType.isAnyType
                    else -> true
                }
            }
        }
    }

    public object ALL : KaSuperTypesFilter {
        override fun filter(analysisSession: KaSession, superType: KaType, symbol: KaClassOrObjectSymbol): Boolean {
            return true
        }
    }

    public object NONE : KaSuperTypesFilter {
        override fun filter(analysisSession: KaSession, superType: KaType, symbol: KaClassOrObjectSymbol): Boolean {
            return false
        }
    }

    public companion object {
        public operator fun invoke(predicate: KaSession.(type: KaType, symbol: KaClassOrObjectSymbol) -> Boolean): KaSuperTypesFilter {
            return KaSuperTypesFilter { analysisSession, superType, symbol -> predicate(analysisSession, superType, symbol) }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaSuperTypesFilter' instead", ReplaceWith("KaSuperTypesFilter"))
public typealias KtSuperTypesFilter = KaSuperTypesFilter