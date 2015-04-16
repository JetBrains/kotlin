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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.load.kotlin.BinaryClassAnnotationAndConstantLoaderImpl
import org.jetbrains.kotlin.load.kotlin.JavaFlexibleTypeCapabilitiesDeserializer
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil

public fun DeserializerForDecompiler(classFile: VirtualFile): DeserializerForDecompiler {
    val kotlinClass = KotlinBinaryClassCache.getKotlinBinaryClass(classFile)
    assert(kotlinClass != null) { "Decompiled data factory shouldn't be called on an unsupported file: " + classFile }
    val packageFqName = kotlinClass!!.getClassId().getPackageFqName()
    return DeserializerForDecompiler(classFile.getParent()!!, packageFqName)
}

public class DeserializerForDecompiler(
        packageDirectory: VirtualFile,
        directoryPackageFqName: FqName
) : DeserializerForDecompilerBase(packageDirectory, directoryPackageFqName) {

    private val classFinder = DirectoryBasedClassFinder(packageDirectory, directoryPackageFqName)

    override val classDataFinder = DirectoryBasedDataFinder(classFinder, LOG)

    override val annotationAndConstantLoader =
            BinaryClassAnnotationAndConstantLoaderImpl(moduleDescriptor, storageManager, classFinder, LoggingErrorReporter(LOG))

    override val deserializationComponents: DeserializationComponents = DeserializationComponents(
            storageManager, moduleDescriptor, classDataFinder, annotationAndConstantLoader, packageFragmentProvider,
            ResolveEverythingToKotlinAnyLocalClassResolver, JavaFlexibleTypeCapabilitiesDeserializer, ClassDescriptorFactory.EMPTY
    )

    override fun resolveDeclarationsInPackage(packageFqName: FqName): Collection<DeclarationDescriptor> {
        assert(packageFqName == directoryPackageFqName) { "Was called for $packageFqName but only $directoryPackageFqName is expected" }
        val binaryClassForPackageClass = classFinder.findKotlinClass(PackageClassUtils.getPackageClassId(packageFqName))
        val annotationData = binaryClassForPackageClass?.getClassHeader()?.annotationData
        if (annotationData == null) {
            LOG.error("Could not read annotation data for $packageFqName from ${binaryClassForPackageClass?.getClassId()}")
            return emptyList()
        }
        val packageData = JvmProtoBufUtil.readPackageDataFrom(annotationData)
        val membersScope = DeserializedPackageMemberScope(
                createDummyPackageFragment(packageFqName),
                packageData.getPackageProto(),
                packageData.getNameResolver(),
                deserializationComponents
        ) { emptyList() }
        return membersScope.getDescriptors()
    }

    companion object {
        private val LOG = Logger.getInstance(javaClass<DeserializerForDecompiler>())
    }
}
