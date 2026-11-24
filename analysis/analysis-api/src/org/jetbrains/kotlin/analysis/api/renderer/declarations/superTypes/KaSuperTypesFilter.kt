/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.KaSpi
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.classId
import org.jetbrains.kotlin.name.StandardClassIds

@KaSpi
@KaExperimentalApi
public fun interface KaSuperTypesFilter {
    public fun filter(analysisSession: KaSession, superType: KaType, symbol: KaClassSymbol): Boolean

    @KaExperimentalApi
    public object NO_DEFAULT_TYPES : KaSuperTypesFilter {
        override fun filter(analysisSession: KaSession, superType: KaType, symbol: KaClassSymbol): Boolean {
            context(analysisSession) {
                if (superType.classId == KaStandardTypeClassIds.ANY) {
                    return false
                }

                if (symbol.classKind == KaClassKind.ANNOTATION_CLASS && superType.classId == StandardClassIds.Annotation) {
                    return false
                }

                if (symbol.classKind == KaClassKind.ENUM_CLASS && superType.classId == StandardClassIds.Enum) {
                    return false
                }

                return true
            }
        }
    }

    @KaExperimentalApi
    public object NO_ANY_FOR_INTERFACES : KaSuperTypesFilter {
        override fun filter(analysisSession: KaSession, superType: KaType, symbol: KaClassSymbol): Boolean {
            context(analysisSession) {
                return when (symbol.classKind) {
                    KaClassKind.INTERFACE -> superType.classId != KaStandardTypeClassIds.ANY
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
