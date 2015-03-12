/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.load.kotlin.BinaryClassAnnotationAndConstantLoaderImpl
import org.jetbrains.kotlin.load.kotlin.JavaFlexibleTypeCapabilitiesDeserializer
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.util.Collections

public fun DeserializerForDecompiler(classFile: VirtualFile): DeserializerForDecompiler {
    val kotlinClass = KotlinBinaryClassCache.getKotlinBinaryClass(classFile)
    assert(kotlinClass != null) { "Decompiled data factory shouldn't be called on an unsupported file: " + classFile }
    val packageFqName = kotlinClass!!.getClassId().getPackageFqName()
    return DeserializerForDecompiler(classFile.getParent()!!, packageFqName)
}

public class DeserializerForDecompiler(val packageDirectory: VirtualFile, val directoryPackageFqName: FqName) : ResolverForDecompiler {

    private val moduleDescriptor =
            ModuleDescriptorImpl(Name.special("<module for building decompiled sources>"), listOf(), PlatformToKotlinClassMap.EMPTY)

    private fun createDummyModule(name: String) = ModuleDescriptorImpl(Name.special("<$name>"), listOf(), PlatformToKotlinClassMap.EMPTY)

    override fun resolveTopLevelClass(classId: ClassId) = deserializationComponents.deserializeClass(classId)

    override fun resolveDeclarationsInPackage(packageFqName: FqName): Collection<DeclarationDescriptor> {
        assert(packageFqName == directoryPackageFqName, "Was called for $packageFqName but only $directoryPackageFqName is expected.")
        val binaryClassForPackageClass = classFinder.findKotlinClass(PackageClassUtils.getPackageClassId(packageFqName))
        val annotationData = binaryClassForPackageClass?.getClassHeader()?.annotationData
        if (annotationData == null) {
            LOG.error("Could not read annotation data for $packageFqName from ${binaryClassForPackageClass?.getClassId()}")
            return Collections.emptyList()
        }
        val packageData = JvmProtoBufUtil.readPackageDataFrom(annotationData)
        val membersScope = DeserializedPackageMemberScope(
                createDummyPackageFragment(packageFqName),
                packageData.getPackageProto(),
                packageData.getNameResolver(),
                deserializationComponents
        ) { listOf() }
        return membersScope.getDescriptors()
    }

    private val classFinder = DirectoryBasedClassFinder(packageDirectory, directoryPackageFqName)
    private val classDataFinder = DirectoryBasedDataFinder(classFinder, LOG)

    private val storageManager = LockBasedStorageManager.NO_LOCKS

    private val annotationAndConstantLoader =
            BinaryClassAnnotationAndConstantLoaderImpl(moduleDescriptor, storageManager, classFinder, LoggingErrorReporter(LOG))

    private val packageFragmentProvider = object : PackageFragmentProvider {
        override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
            return listOf(createDummyPackageFragment(fqName))
        }

        override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
            throw UnsupportedOperationException("This method is not supposed to be called.")
        }
    }

    {
        moduleDescriptor.initialize(packageFragmentProvider)
        moduleDescriptor.addDependencyOnModule(moduleDescriptor)
        moduleDescriptor.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule())
        val moduleContainingMissingDependencies = createDummyModule("module containing missing dependencies for decompiled sources")
        moduleContainingMissingDependencies.addDependencyOnModule(moduleContainingMissingDependencies)
        moduleContainingMissingDependencies.initialize(
                PackageFragmentProviderForMissingDependencies(moduleContainingMissingDependencies)
        )
        moduleDescriptor.addDependencyOnModule(moduleContainingMissingDependencies)
        moduleDescriptor.seal()
        moduleContainingMissingDependencies.seal()
    }

    private val deserializationComponents = DeserializationComponents(
            storageManager, moduleDescriptor, classDataFinder, annotationAndConstantLoader, packageFragmentProvider,
            ResolveEverythingToKotlinAnyLocalClassResolver, JavaFlexibleTypeCapabilitiesDeserializer
    )

    private fun createDummyPackageFragment(fqName: FqName): MutablePackageFragmentDescriptor {
        return MutablePackageFragmentDescriptor(moduleDescriptor, fqName)
    }

    default object {
        private val LOG = Logger.getInstance(javaClass<DeserializerForDecompiler>())
    }
}
