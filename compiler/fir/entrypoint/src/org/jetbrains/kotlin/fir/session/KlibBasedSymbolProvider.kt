/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.*
import org.jetbrains.kotlin.fir.isNewPlaceForBodyGeneration
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.utils.SmartList
import java.nio.file.Paths

class KlibBasedSymbolProvider(
    session: FirSession,
    moduleDataProvider: ModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    private val resolvedLibraries: Collection<KotlinLibrary>,
    defaultDeserializationOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library
) : MetadataLibraryBasedSymbolProvider<KotlinLibrary>(
    session,
    moduleDataProvider,
    kotlinScopeProvider,
    defaultDeserializationOrigin
) {
    private val moduleHeaders by lazy {
        resolvedLibraries.associate { it to parseModuleHeader(it.moduleHeaderData) }
    }

    override val fragmentNamesInLibraries: Map<String, List<KotlinLibrary>> by lazy {
        buildMap<String, SmartList<KotlinLibrary>> {
            for ((library, header) in moduleHeaders) {
                for (fragmentName in header.packageFragmentNameList) {
                    getOrPut(fragmentName) { SmartList() }
                        .add(library)
                }
            }
        }
    }

    override val knownPackagesInLibraries: Set<FqName> by lazy {
        buildSet<FqName> {
            for ((_, header) in moduleHeaders) {
                for (fragmentName in header.packageFragmentNameList) {
                    var curPackage = FqName(fragmentName)
                    while (!curPackage.isRoot) {
                        add(curPackage)
                        curPackage = curPackage.parent()
                    }
                }
            }
        }
    }

    override fun moduleData(library: KotlinLibrary): FirModuleData? {
        val libraryPath = Paths.get(library.libraryFile.path)
        return moduleDataProvider.getModuleData(libraryPath)
    }

    override fun createDeserializedContainerSource(
        resolvedLibrary: KotlinLibrary,
        packageFqName: FqName
    ) = KlibDeserializedContainerSource(
        resolvedLibrary,
        moduleHeaders[resolvedLibrary]!!,
        deserializationConfiguration,
        packageFqName
    )
}
