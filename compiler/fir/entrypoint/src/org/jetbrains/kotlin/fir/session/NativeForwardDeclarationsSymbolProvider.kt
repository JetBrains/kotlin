/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.getDeprecationsProvider
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProviderWithoutCallables
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.includedForwardDeclarations
import org.jetbrains.kotlin.library.isInterop
import org.jetbrains.kotlin.library.metadata.impl.ForwardDeclarationKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class NativeForwardDeclarationsSymbolProvider(
    session: FirSession,
    private val forwardDeclarationsModuleData: FirModuleData,
    private val kotlinScopeProvider: FirKotlinScopeProvider,
    private val kotlinLibraries: Collection<KotlinLibrary>,
) : FirSymbolProvider(session) {
    private companion object {
        private val validPackages = ForwardDeclarationKind.packageFqNameToKind.keys
    }

    private val includedForwardDeclarations: Set<ClassId> by lazy {
        buildSet {
            for (library in kotlinLibraries) {
                if (!library.isInterop) continue

                for (fqName in library.includedForwardDeclarations) {
                    val classId = ClassId.topLevel(FqName(fqName))
                    if (classId.packageFqName in validPackages) add(classId)
                }
            }
        }
    }

    private val includedForwardDeclarationsByPackage: Map<FqName, Set<String>> by lazy {
        buildMap<FqName, MutableSet<String>> {
            for (classId in includedForwardDeclarations) {
                getOrPut(classId.packageFqName) { mutableSetOf() }
                    .add(classId.shortClassName.asString())
            }
        }
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId !in includedForwardDeclarations) return null
        if (classId.isNestedClass) return null

        return syntheticForwardDeclarationClassCache.getValue(classId)
    }

    private val syntheticForwardDeclarationClassCache =
        session.firCachesFactory.createCache(::createSyntheticForwardDeclarationClass)

    private fun createSyntheticForwardDeclarationClass(classId: ClassId): FirClassLikeSymbol<*>? {
        val forwardDeclarationKind = ForwardDeclarationKind.packageFqNameToKind[classId.packageFqName] ?: return null

        val symbol = FirRegularClassSymbol(classId)

        buildRegularClass {
            moduleData = forwardDeclarationsModuleData
            origin = FirDeclarationOrigin.Synthetic
            check(!classId.isNestedClass) { "Expected top-level class when building forward declaration, got $classId" }
            name = classId.shortClassName
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            ).apply {
                // This will be wrong if we support exported forward declarations.
                // See https://youtrack.jetbrains.com/issue/KT-51377 for more details.
                isExpect = false

                isActual = false
                isCompanion = false
                isInner = false
                isData = false
                isInline = false
                isExternal = false
                isFun = false
            }
            classKind = forwardDeclarationKind.classKind
            scopeProvider = kotlinScopeProvider
            this.symbol = symbol

            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES

            superTypeRefs += buildResolvedTypeRef {
                type = ConeClassLikeLookupTagImpl(forwardDeclarationKind.superClassId)
                    .constructClassType(emptyArray(), isNullable = false)
            }
        }.apply {
            replaceDeprecationsProvider(getDeprecationsProvider(session))
        }

        return symbol
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
        if (fqName in includedForwardDeclarationsByPackage) {
            return fqName
        }
        return null
    }

    override val symbolNamesProvider: FirSymbolNamesProvider = object : FirSymbolNamesProviderWithoutCallables() {
        override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<String> =
            includedForwardDeclarationsByPackage[packageFqName].orEmpty()
    }
}
