/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.declarations.builder.buildResolvedImport
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

// Note: at this moment we create this scope for enum classes only,
// and only enum entry symbols are allowed to be processed
class FirWhenSubjectImportingScope(
    classId: ClassId, session: FirSession, scopeSession: ScopeSession
) : FirExplicitStarImportingScope(
    listOf(buildResolvedImportByClassId(classId)),
    session, scopeSession
) {
    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        super.processPropertiesByName(name) {
            if (it is FirEnumEntrySymbol) {
                processor(it)
            }
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
    }

    companion object {
        private fun buildResolvedImportByClassId(classId: ClassId) = buildResolvedImport {
            delegate = buildImport {
                importedFqName = classId.asSingleFqName()
                isAllUnder = true
            }
            packageFqName = classId.packageFqName
            relativeParentClassName = classId.relativeClassName
        }
    }
}