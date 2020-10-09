/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.klib

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DeserializerForDecompilerBase
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LoggingErrorReporter
import org.jetbrains.kotlin.idea.decompiler.textBuilder.ResolveEverythingToKotlinAnyLocalClassifierResolver
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.metadata.KlibMetadataClassDataFinder
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.sam.SamConversionResolverImpl
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope

class KlibMetadataDeserializerForDecompiler(
    packageFqName: FqName,
    private val proto: ProtoBuf.PackageFragment,
    private val nameResolver: NameResolver,
    serializerProtocol: SerializerExtensionProtocol,
    flexibleTypeDeserializer: FlexibleTypeDeserializer
) : DeserializerForDecompilerBase(packageFqName) {
    override val builtIns: KotlinBuiltIns get() = DefaultBuiltIns.Instance

    override val deserializationComponents: DeserializationComponents

    init {
        val notFoundClasses = NotFoundClasses(storageManager, moduleDescriptor)

        deserializationComponents = DeserializationComponents(
            storageManager, moduleDescriptor, DeserializationConfiguration.Default,
            KlibMetadataClassDataFinder(proto, nameResolver),
            AnnotationAndConstantLoaderImpl(moduleDescriptor, notFoundClasses, serializerProtocol), packageFragmentProvider,
            ResolveEverythingToKotlinAnyLocalClassifierResolver(builtIns), LoggingErrorReporter(LOG),
            LookupTracker.DO_NOTHING, flexibleTypeDeserializer, emptyList(), notFoundClasses, ContractDeserializer.DEFAULT,
            extensionRegistryLite = serializerProtocol.extensionRegistry,
            samConversionResolver = SamConversionResolverImpl(storageManager, samWithReceiverResolvers = emptyList())
        )
    }

    override fun resolveDeclarationsInFacade(facadeFqName: FqName): List<DeclarationDescriptor> {
        assert(facadeFqName == directoryPackageFqName) {
            "Was called for $facadeFqName; only members of $directoryPackageFqName package are expected."
        }

        val membersScope = DeserializedPackageMemberScope(
            createDummyPackageFragment(facadeFqName),
            proto.`package`,
            nameResolver,
            KlibMetadataVersion.INSTANCE,
            containerSource = null,
            components = deserializationComponents
        ) { emptyList() }

        return membersScope.getContributedDescriptors().toList()
    }

    companion object {
        private val LOG = Logger.getInstance(KlibMetadataDeserializerForDecompiler::class.java)
    }
}
