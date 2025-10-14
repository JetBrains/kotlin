/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.FirTypeDeserializer
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.library.metadata.getIncompatibility
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.util.toKlibMetadataVersion
import org.jetbrains.kotlin.utils.SmartList
import java.nio.file.Paths

class KlibBasedSymbolProvider(
    session: FirSession,
    moduleDataProvider: ModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    private val resolvedLibraries: Collection<KotlinLibrary>,
    defaultDeserializationOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library,
    flexibleTypeFactory: FirTypeDeserializer.FlexibleTypeFactory = FirTypeDeserializer.FlexibleTypeFactory.Default,
) : MetadataLibraryBasedSymbolProvider<KotlinLibrary>(
    session,
    moduleDataProvider,
    kotlinScopeProvider,
    flexibleTypeFactory,
    defaultDeserializationOrigin,
    metadataProvider = { it.metadata },
) {
    private val ownMetadataVersion: MetadataVersion = session.languageVersionSettings.languageVersion.toKlibMetadataVersion()

    private val KotlinLibrary.incompatibility: IncompatibleVersionErrorData<MetadataVersion>?
        get() {
            if (session.languageVersionSettings.getFlag(AnalysisFlags.skipMetadataVersionCheck)) return null
            return getIncompatibility(ownMetadataVersion)
        }


    private val moduleHeaders by lazy {
        resolvedLibraries.associateWith { parseModuleHeader(metadataProvider(it).moduleHeaderData) }
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
    ): KlibDeserializedContainerSource = KlibDeserializedContainerSource(
        resolvedLibrary,
        moduleHeaders[resolvedLibrary]!!,
        deserializationConfiguration,
        packageFqName,
        resolvedLibrary.incompatibility
    )
}
