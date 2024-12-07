/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.StandardClassIds

@KaExperimentalApi
public fun interface KaSuperTypesFilter {
    public fun filter(analysisSession: KaSession, superType: KaType, symbol: KaClassSymbol): Boolean

    @KaExperimentalApi
    public object NO_DEFAULT_TYPES : KaSuperTypesFilter {
        override fun filter(analysisSession: KaSession, superType: KaType, symbol: KaClassSymbol): Boolean {
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

    @KaExperimentalApi
    public object NO_ANY_FOR_INTERFACES : KaSuperTypesFilter {
        override fun filter(analysisSession: KaSession, superType: KaType, symbol: KaClassSymbol): Boolean {
            with(analysisSession) {
                return when (symbol.classKind) {
                    KaClassKind.INTERFACE -> !superType.isAnyType
                    else -> true
                }
            }
        }
    }

    @KaExperimentalApi
    public object ALL : KaSuperTypesFilter {
        override fun filter(analysisSession: KaSession, superType: KaType, symbol: KaClassSymbol): Boolean {
            return true
        }
    }

    @KaExperimentalApi
    public object NONE : KaSuperTypesFilter {
        override fun filter(analysisSession: KaSession, superType: KaType, symbol: KaClassSymbol): Boolean {
            return false
        }
    }

    @KaExperimentalApi
    public companion object {
        public operator fun invoke(predicate: KaSession.(type: KaType, symbol: KaClassSymbol) -> Boolean): KaSuperTypesFilter {
            return KaSuperTypesFilter { analysisSession, superType, symbol -> predicate(analysisSession, superType, symbol) }
        }
    }
}
