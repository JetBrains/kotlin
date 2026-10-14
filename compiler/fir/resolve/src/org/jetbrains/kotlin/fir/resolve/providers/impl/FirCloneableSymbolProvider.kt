/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildNamedFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.name.StandardClassIds

@NoMutableState
class FirCloneableSymbolProvider(
    session: FirSession,
    moduleData: FirModuleData,
    scopeProvider: FirScopeProvider,
) : FirSingleClassSymbolProvider(buildCloneableClass(moduleData, session, scopeProvider), session)

private fun buildCloneableClass(
    moduleData: FirModuleData,
    session: FirSession,
    scopeProvider: FirScopeProvider,
): FirRegularClass {
    return buildRegularClass {
        resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
        origin = FirDeclarationOrigin.Library
        this.moduleData = moduleData
        status = FirResolvedDeclarationStatusImpl(
            Visibilities.Public,
            Modality.ABSTRACT,
            EffectiveVisibility.Public
        )

        classKind = ClassKind.INTERFACE
        val classSymbol = FirRegularClassSymbol(StandardClassIds.Cloneable)
        symbol = classSymbol
        superTypeRefs += buildResolvedTypeRef {
            coneType = session.builtinTypes.anyType.coneType
        }

        declarations += buildNamedFunction {
            this.moduleData = moduleData
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            origin = FirDeclarationOrigin.Library
            returnTypeRef = buildResolvedTypeRef {
                coneType = session.builtinTypes.anyType.coneType
            }

            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Protected,
                Modality.OPEN,
                Visibilities.Protected.toEffectiveVisibility(classSymbol)
            )
            isLocal = false

            name = StandardClassIds.Callables.clone.callableName
            symbol = FirNamedFunctionSymbol(StandardClassIds.Callables.clone)
            dispatchReceiverType = this@buildRegularClass.symbol.constructType()
        }

        this.scopeProvider = scopeProvider
        name = StandardClassIds.Cloneable.shortClassName
    }
}
