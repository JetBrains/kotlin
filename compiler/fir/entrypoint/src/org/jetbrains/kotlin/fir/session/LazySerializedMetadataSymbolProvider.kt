/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session


import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.library.MetadataLibrary
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.SmartList

class LazySerializedMetadataLibrary(getSerializedMetadata: () -> SerializedMetadata?): MetadataLibrary {

    private val serializedMetadata by lazy { getSerializedMetadata() }
    private val parts: Map<String, Map<String, ByteArray>> by lazy {
        val result = mutableMapOf<String, Map<String, ByteArray>>()

        serializedMetadata?.fragments?.forEachIndexed { i, fs ->
            val fragmentFqn = serializedMetadata?.fragmentNames?.get(i) ?: return@forEachIndexed
            val partName = fragmentFqn.substringAfterLast(".")
            val numCount = fs.size.toString().length
            fun withLeadingZeros(i: Int) = String.format("%0${numCount}d", i)
            // see MetadataWriterImpl.addMetadata for "inspiration" (still unclear why we need the partName in this form)
            result[fragmentFqn] = fs.withIndex().associate { "${withLeadingZeros(it.index)}_$partName" to it.value }
        }

        result
    }

    val packageFragmentNameList: Collection<String>
        get() = parts.keys

    override val moduleHeaderData: ByteArray
        get() = serializedMetadata?.module ?: ByteArray(0)

    override fun packageMetadataParts(fqName: String): Set<String> {
        return parts[fqName]?.keys ?: emptySet()
    }

    override fun packageMetadata(fqName: String, partName: String): ByteArray {
        return parts[fqName]?.get(partName) ?: error("Metadata not found for package $fqName part $partName")
    }

}

class LazySerializedMetadataSymbolProvider(
    session: FirSession,
    moduleDataProvider: SingleModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    getSerializedMetadata: () -> SerializedMetadata?,
    defaultDeserializationOrigin: FirDeclarationOrigin = FirDeclarationOrigin.CommonArtefact
) : MetadataLibraryBasedSymbolProvider<LazySerializedMetadataLibrary>(
    session,
    moduleDataProvider,
    kotlinScopeProvider,
    defaultDeserializationOrigin
) {
    private val serializedMetadataLibrary = LazySerializedMetadataLibrary(getSerializedMetadata)

    override fun moduleData(library: LazySerializedMetadataLibrary): FirModuleData {
        return moduleDataProvider.allModuleData.single()
    }

    override val fragmentNamesInLibraries: Map<String, List<LazySerializedMetadataLibrary>> by lazy {
        buildMap<String, SmartList<LazySerializedMetadataLibrary>> {
            for (fragmentName in serializedMetadataLibrary.packageFragmentNameList) {
                getOrPut(fragmentName) { SmartList() }
                    .add(serializedMetadataLibrary)
            }
        }
    }

    override val knownPackagesInLibraries: Set<FqName> by lazy {
        buildSet<FqName> {
            for (fragmentName in serializedMetadataLibrary.packageFragmentNameList) {
                var curPackage = FqName(fragmentName)
                while (!curPackage.isRoot) {
                    add(curPackage)
                    curPackage = curPackage.parent()
                }
            }
        }
    }

    override fun createDeserializedContainerSource(resolvedLibrary: LazySerializedMetadataLibrary, packageFqName: FqName): DeserializedContainerSource {
        return KlibDeserializedContainerSource(false, "Package '$packageFqName'", false)
    }
}