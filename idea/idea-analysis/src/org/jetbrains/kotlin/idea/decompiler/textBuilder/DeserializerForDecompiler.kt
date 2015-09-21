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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil

public fun DeserializerForDecompiler(classFile: VirtualFile): DeserializerForDecompiler {
    val kotlinClass = KotlinBinaryClassCache.getKotlinBinaryClass(classFile)
    assert(kotlinClass != null) { "Decompiled data factory shouldn't be called on an unsupported file: " + classFile }
    val packageFqName = kotlinClass!!.classId.packageFqName
    return DeserializerForDecompiler(classFile.parent!!, packageFqName)
}

public class DeserializerForDecompiler(
        packageDirectory: VirtualFile,
        directoryPackageFqName: FqName
) : DeserializerForDecompilerBase(packageDirectory, directoryPackageFqName) {

    private val classFinder = DirectoryBasedClassFinder(packageDirectory, directoryPackageFqName)

    override val classDataFinder = DirectoryBasedDataFinder(classFinder, LOG)

    private val errorReporter = LoggingErrorReporter(LOG)

    override val annotationAndConstantLoader =
            BinaryClassAnnotationAndConstantLoaderImpl(moduleDescriptor, storageManager, classFinder, errorReporter)

    override val deserializationComponents: DeserializationComponents = DeserializationComponents(
            storageManager, moduleDescriptor, classDataFinder, annotationAndConstantLoader, packageFragmentProvider,
            ResolveEverythingToKotlinAnyLocalClassResolver, errorReporter, JavaFlexibleTypeCapabilitiesDeserializer,
            ClassDescriptorFactory.EMPTY
    )

    override fun resolveDeclarationsInFacade(facadeFqName: FqName): Collection<DeclarationDescriptor> {
        val packageFqName = facadeFqName.parent()
        assert(packageFqName == directoryPackageFqName) {
            "Was called for $facadeFqName; only members of $directoryPackageFqName package are expected."
        }
        val binaryClassForPackageClass = classFinder.findKotlinClass(ClassId.topLevel(facadeFqName))
        val header = binaryClassForPackageClass?.classHeader
        val annotationData = header?.annotationData
        val strings = header?.strings
        if (annotationData == null || strings == null) {
            LOG.error("Could not read annotation data for $facadeFqName from ${binaryClassForPackageClass?.getClassId()}")
            return emptyList()
        }
        val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(annotationData, strings)
        val membersScope = DeserializedPackageMemberScope(
                createDummyPackageFragment(packageFqName), packageProto, nameResolver, deserializationComponents
        ) { emptyList() }
        return membersScope.getDescriptors()
    }

    companion object {
        private val LOG = Logger.getInstance(DeserializerForDecompiler::class.java)
    }
}
