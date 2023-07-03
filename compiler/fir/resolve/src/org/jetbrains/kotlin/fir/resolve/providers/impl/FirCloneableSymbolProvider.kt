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
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProviderWithoutCallables
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

@NoMutableState
class FirCloneableSymbolProvider(
    session: FirSession,
    moduleData: FirModuleData,
    scopeProvider: FirScopeProvider,
) : FirSymbolProvider(session) {
    private val klass = buildRegularClass {
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
            type = session.builtinTypes.anyType.type
        }

        declarations += buildSimpleFunction {
            this.moduleData = moduleData
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            origin = FirDeclarationOrigin.Library
            returnTypeRef = buildResolvedTypeRef {
                type = session.builtinTypes.anyType.type
            }

            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Protected,
                Modality.OPEN,
                Visibilities.Protected.toEffectiveVisibility(classSymbol)
            )

            name = StandardClassIds.Callables.clone.callableName
            symbol = FirNamedFunctionSymbol(StandardClassIds.Callables.clone)
            dispatchReceiverType = this@buildRegularClass.symbol.constructType(emptyArray(), isNullable = false)
        }

        this.scopeProvider = scopeProvider
        name = StandardClassIds.Cloneable.shortClassName

    }

    override val symbolNamesProvider: FirSymbolNamesProvider = object : FirSymbolNamesProviderWithoutCallables() {
        override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<String> =
            if (packageFqName == StandardClassIds.Cloneable.packageFqName) {
                setOf(StandardClassIds.Cloneable.shortClassName.asString())
            } else emptySet()
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        return if (classId == StandardClassIds.Cloneable) klass.symbol else null
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
    }

    override fun getPackage(fqName: FqName): FqName? {
        return null
    }
}
