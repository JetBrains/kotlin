/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session


import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.FirTypeDeserializer
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.SmartList

class KlibIcCacheBasedSymbolProvider(
    session: FirSession,
    moduleDataProvider: SingleModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    private val icData: KlibIcData,
    defaultDeserializationOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Precompiled,
    flexibleTypeFactory: FirTypeDeserializer.FlexibleTypeFactory = FirTypeDeserializer.FlexibleTypeFactory.Default,
) : MetadataLibraryBasedSymbolProvider<KlibIcData>(
    session,
    moduleDataProvider,
    kotlinScopeProvider,
    flexibleTypeFactory,
    defaultDeserializationOrigin,
    metadataProvider = { it.metadata },
) {
    override fun moduleData(library: KlibIcData): FirModuleData {
        return moduleDataProvider.allModuleData.single()
    }

    override val fragmentNamesInLibraries: Map<String, List<KlibIcData>> by lazy {
        buildMap<String, SmartList<KlibIcData>> {
            // TODO this cast can be removed after KT-89276
            for (fragmentName in (icData.metadata as KlibIcMetadataComponent).packageFragmentNameList) {
                getOrPut(fragmentName) { SmartList() }
                    .add(icData)
            }
        }
    }

    override val knownPackagesInLibraries: Set<FqName> by lazy {
        buildSet<FqName> {
            // TODO this cast can be removed after KT-89276
            for (fragmentName in (icData.metadata as KlibIcMetadataComponent).packageFragmentNameList) {
                var curPackage = FqName(fragmentName)
                while (!curPackage.isRoot) {
                    add(curPackage)
                    curPackage = curPackage.parent()
                }
            }
        }
    }

    override fun createDeserializedContainerSource(resolvedLibrary: KlibIcData, packageFqName: FqName): DeserializedContainerSource {
        return KlibIcDeserializedContainerSource(resolvedLibrary, packageFqName)
    }
}
