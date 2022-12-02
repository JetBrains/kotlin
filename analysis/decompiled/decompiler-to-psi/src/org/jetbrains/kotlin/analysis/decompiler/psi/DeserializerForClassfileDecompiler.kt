/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.decompiler.stub.file.DirectoryBasedClassFinder
import org.jetbrains.kotlin.analysis.decompiler.stub.file.DirectoryBasedDataFinder
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.contracts.ContractDeserializerImpl
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.sam.SamConversionResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope

fun DeserializerForClassfileDecompiler(classFile: VirtualFile): DeserializerForClassfileDecompiler {
    val kotlinClassHeaderInfo =
        ClsKotlinBinaryClassCache.getInstance().getKotlinBinaryClassHeaderData(classFile)
            ?: error("Decompiled data factory shouldn't be called on an unsupported file: $classFile")
    val packageFqName = kotlinClassHeaderInfo.classId.packageFqName
    return DeserializerForClassfileDecompiler(classFile.parent!!, packageFqName, kotlinClassHeaderInfo.metadataVersion)
}

class DeserializerForClassfileDecompiler(
    packageDirectory: VirtualFile,
    directoryPackageFqName: FqName,
    private val jvmMetadataVersion: JvmMetadataVersion
) : DeserializerForDecompilerBase(directoryPackageFqName) {
    override val builtIns: KotlinBuiltIns get() = DefaultBuiltIns.Instance

    private val classFinder = DirectoryBasedClassFinder(packageDirectory, directoryPackageFqName)

    override val deserializationComponents: DeserializationComponents

    init {
        val classDataFinder = DirectoryBasedDataFinder(classFinder, LOG, jvmMetadataVersion)
        val notFoundClasses = NotFoundClasses(storageManager, moduleDescriptor)
        val annotationAndConstantLoader = createBinaryClassAnnotationAndConstantLoader(
            moduleDescriptor, notFoundClasses, storageManager, classFinder, jvmMetadataVersion
        )

        val configuration = object : DeserializationConfiguration {
            override val readDeserializedContracts: Boolean = true
            override val preserveDeclarationsOrdering: Boolean = true
        }

        deserializationComponents = DeserializationComponents(
            storageManager, moduleDescriptor, configuration, classDataFinder, annotationAndConstantLoader,
            packageFragmentProvider, ResolveEverythingToKotlinAnyLocalClassifierResolver(builtIns), LoggingErrorReporter(LOG),
            LookupTracker.DO_NOTHING, JavaFlexibleTypeDeserializer, emptyList(), notFoundClasses,
            ContractDeserializerImpl(configuration, storageManager),
            extensionRegistryLite = JvmProtoBufUtil.EXTENSION_REGISTRY,
            samConversionResolver = SamConversionResolverImpl(storageManager, samWithReceiverResolvers = emptyList())
        )
    }

    override fun resolveDeclarationsInFacade(facadeFqName: FqName): List<DeclarationDescriptor> {
        val packageFqName = facadeFqName.parent()
        assert(packageFqName == directoryPackageFqName) {
            "Was called for $facadeFqName; only members of $directoryPackageFqName package are expected."
        }
        val binaryClassForPackageClass = classFinder.findKotlinClass(ClassId.topLevel(facadeFqName), jvmMetadataVersion)
        val header = binaryClassForPackageClass?.classHeader
        val annotationData = header?.data
        val strings = header?.strings
        if (annotationData == null || strings == null) {
            LOG.error("Could not read annotation data for $facadeFqName from ${binaryClassForPackageClass?.classId}")
            return emptyList()
        }
        val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(annotationData, strings)
        val dummyPackageFragment = createDummyPackageFragment(header.packageName?.let(::FqName) ?: facadeFqName.parent())
        val membersScope = DeserializedPackageMemberScope(
            dummyPackageFragment,
            packageProto, nameResolver, header.metadataVersion,
            JvmPackagePartSource(binaryClassForPackageClass, packageProto, nameResolver), deserializationComponents,
            "scope of dummyPackageFragment ${dummyPackageFragment.fqName} in module $moduleDescriptor @DeserializerForClassfileDecompiler"
        ) { emptyList() }
        return membersScope.getContributedDescriptors().toList()
    }

    companion object {
        private val LOG = Logger.getInstance(DeserializerForClassfileDecompiler::class.java)
    }
}