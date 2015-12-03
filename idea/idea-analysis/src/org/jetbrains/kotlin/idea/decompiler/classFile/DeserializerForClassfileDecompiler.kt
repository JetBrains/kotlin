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

package org.jetbrains.kotlin.idea.decompiler.classFile

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DeserializerForDecompilerBase
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LoggingErrorReporter
import org.jetbrains.kotlin.idea.decompiler.textBuilder.ResolveEverythingToKotlinAnyLocalClassResolver
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.serialization.ClassDataWithSource
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil

public fun DeserializerForClassfileDecompiler(classFile: VirtualFile): DeserializerForClassfileDecompiler {
    val kotlinClass = KotlinBinaryClassCache.getKotlinBinaryClass(classFile)
    assert(kotlinClass != null) { "Decompiled data factory shouldn't be called on an unsupported file: " + classFile }
    val packageFqName = kotlinClass!!.classId.packageFqName
    return DeserializerForClassfileDecompiler(classFile.parent!!, packageFqName)
}

public class DeserializerForClassfileDecompiler(
        packageDirectory: VirtualFile,
        directoryPackageFqName: FqName
) : DeserializerForDecompilerBase(packageDirectory, directoryPackageFqName) {
    override val targetPlatform: TargetPlatform get() = JvmPlatform

    private val classFinder = DirectoryBasedClassFinder(packageDirectory, directoryPackageFqName)

    private val classDataFinder = DirectoryBasedDataFinder(classFinder, LOG)

    private val errorReporter = LoggingErrorReporter(LOG)

    private val annotationAndConstantLoader =
            BinaryClassAnnotationAndConstantLoaderImpl(moduleDescriptor, storageManager, classFinder, errorReporter)

    override val deserializationComponents: DeserializationComponents = DeserializationComponents(
            storageManager, moduleDescriptor, classDataFinder, annotationAndConstantLoader, packageFragmentProvider,
            ResolveEverythingToKotlinAnyLocalClassResolver(targetPlatform.builtIns), errorReporter,
            LookupTracker.DO_NOTHING, JavaFlexibleTypeCapabilitiesDeserializer, ClassDescriptorFactory.EMPTY
    )

    override fun resolveDeclarationsInFacade(facadeFqName: FqName): List<DeclarationDescriptor> {
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
        return membersScope.getContributedDescriptors().toList()
    }

    companion object {
        private val LOG = Logger.getInstance(DeserializerForClassfileDecompiler::class.java)
    }
}

class DirectoryBasedClassFinder(
        val packageDirectory: VirtualFile,
        val directoryPackageFqName: FqName
) : KotlinClassFinder {
    override fun findKotlinClass(javaClass: JavaClass) = findKotlinClass(javaClass.classId)

    override fun findKotlinClass(classId: ClassId): KotlinJvmBinaryClass? {
        if (classId.getPackageFqName() != directoryPackageFqName) {
            return null
        }
        val targetName = classId.getRelativeClassName().pathSegments().joinToString("$", postfix = ".class")
        val virtualFile = packageDirectory.findChild(targetName)
        if (virtualFile != null && isKotlinWithCompatibleAbiVersion(virtualFile)) {
            return KotlinBinaryClassCache.getKotlinBinaryClass(virtualFile)
        }
        return null
    }
}

class DirectoryBasedDataFinder(
        val classFinder: DirectoryBasedClassFinder,
        val log: Logger
) : ClassDataFinder {
    override fun findClassData(classId: ClassId): ClassDataWithSource? {
        val binaryClass = classFinder.findKotlinClass(classId) ?: return null
        val classHeader = binaryClass.classHeader
        val data = classHeader.annotationData
        if (data == null) {
            log.error("Annotation data missing for ${binaryClass.classId}")
            return null
        }
        val strings = classHeader.strings
        if (strings == null) {
            log.error("String table not found in class ${binaryClass.classId}")
            return null
        }

        return ClassDataWithSource(JvmProtoBufUtil.readClassDataFrom(data, strings))
    }
}


private val JavaClass.classId: ClassId
    get() {
        val outer = getOuterClass()
        return if (outer == null) ClassId.topLevel(getFqName()!!) else outer.classId.createNestedClassId(getName())
    }
