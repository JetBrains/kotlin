/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.libraries

import org.jetbrains.jet.descriptors.serialization.ClassId
import org.jetbrains.jet.descriptors.serialization.ClassDataFinder
import org.jetbrains.jet.descriptors.serialization.JavaProtoBufUtil
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.storage.LockBasedStorageManager
import java.util.Collections
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jet.lang.resolve.kotlin.KotlinBinaryClassCache
import org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.jet.lang.resolve.kotlin.AnnotationDescriptorLoader
import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.jet.lang.resolve.kotlin.KotlinClassFinder
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe
import org.jetbrains.jet.lang.resolve.kotlin.DescriptorLoadersStorage
import org.jetbrains.jet.lang.resolve.kotlin.ConstantDescriptorLoader
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.descriptors.serialization.context.DeserializationGlobalContext
import org.jetbrains.jet.lang.PlatformToKotlinClassMap
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.descriptors.serialization.ClassData

public fun DeserializerForDecompiler(classFile: VirtualFile): DeserializerForDecompiler {
    val kotlinClass = KotlinBinaryClassCache.getKotlinBinaryClass(classFile)
    assert(kotlinClass != null) { "Decompiled data factory shouldn't be called on an unsupported file: " + classFile }
    val classFqName = kotlinClass!!.getClassName().getFqNameForClassNameWithoutDollars()
    val packageFqName = classFqName.parent()
    return DeserializerForDecompiler(classFile.getParent()!!, packageFqName)
}

public class DeserializerForDecompiler(val packageDirectory: VirtualFile, val directoryPackageFqName: FqName) : ResolverForDecompiler {

    private val moduleDescriptor =
            ModuleDescriptorImpl(Name.special("<module for building decompiled sources>"), listOf(), PlatformToKotlinClassMap.EMPTY)

    override fun resolveTopLevelClass(classFqName: FqName) = deserializationContext.classDeserializer.deserializeClass(classFqName.toClassId())

    override fun resolveDeclarationsInPackage(packageFqName: FqName): Collection<DeclarationDescriptor> {
        assert(packageFqName == directoryPackageFqName, "Was called for $packageFqName but only $directoryPackageFqName is expected.")
        val packageClassFqName = PackageClassUtils.getPackageClassFqName(packageFqName)
        val binaryClassForPackageClass = localClassFinder.findKotlinClass(packageClassFqName)
        val annotationData = binaryClassForPackageClass?.getClassHeader()?.annotationData
        if (annotationData == null) {
            LOG.error("Could not read annotation data for $packageFqName from ${binaryClassForPackageClass?.getClassName()}")
            return Collections.emptyList()
        }
        val membersScope = DeserializedPackageMemberScope(
                createDummyPackageFragment(packageFqName),
                JavaProtoBufUtil.readPackageDataFrom(annotationData),
                deserializationContext
        ) { listOf() }
        return membersScope.getAllDescriptors()
    }

    private val localClassFinder = object: KotlinClassFinder {
        override fun findKotlinClass(fqName: FqName) = findKotlinClass(fqName.toClassId())
        override fun findKotlinClass(javaClass: JavaClass) = findKotlinClass(javaClass.getFqName()!!)

        fun findKotlinClass(classId: ClassId): KotlinJvmBinaryClass? {
            if (classId.getPackageFqName() != directoryPackageFqName) {
                return null
            }
            val segments = DeserializedResolverUtils.kotlinFqNameToJavaFqName(classId.getRelativeClassName()).pathSegments()
            val targetName = segments.makeString("$", postfix = ".class")
            val virtualFile = packageDirectory.findChild(targetName)
            if (virtualFile != null && isKotlinCompiledFile(virtualFile)) {
                return KotlinBinaryClassCache.getKotlinBinaryClass(virtualFile)
            }
            return null
        }
    }
    private val storageManager = LockBasedStorageManager.NO_LOCKS

    private val loadersStorage = DescriptorLoadersStorage(storageManager);
    {
        loadersStorage.setModule(moduleDescriptor)
        loadersStorage.setErrorReporter(LOGGING_REPORTER)
    }

    private val annotationLoader = AnnotationDescriptorLoader();
    {
        annotationLoader.setModule(moduleDescriptor)
        annotationLoader.setKotlinClassFinder(localClassFinder)
        annotationLoader.setErrorReporter(LOGGING_REPORTER)
        annotationLoader.setStorage(loadersStorage)
    }

    private val constantLoader = ConstantDescriptorLoader();
    {
        constantLoader.setKotlinClassFinder(localClassFinder)
        constantLoader.setErrorReporter(LOGGING_REPORTER)
        constantLoader.setStorage(loadersStorage)
    }

    private val classDataFinder = object : ClassDataFinder {
        override fun findClassData(classId: ClassId): ClassData? {
            val binaryClass = localClassFinder.findKotlinClass(classId) ?: return null
            val data = binaryClass.getClassHeader().annotationData
            if (data == null) {
                LOG.error("Annotation data missing for ${binaryClass.getClassName()}")
                return null
            }
            return JavaProtoBufUtil.readClassDataFrom(data)
        }
    }

    private val packageFragmentProvider = object : PackageFragmentProvider {
        override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
            return listOf(createDummyPackageFragment(fqName))
        }

        override fun getSubPackagesOf(fqName: FqName): Collection<FqName> {
            throw UnsupportedOperationException("This method is not supposed to be called.")
        }
    }

    {
        moduleDescriptor.addFragmentProvider(DependencyKind.BUILT_INS,
                                             KotlinBuiltIns.getInstance().getBuiltInsModule().getPackageFragmentProvider())
        moduleDescriptor.addFragmentProvider(DependencyKind.SOURCES, packageFragmentProvider)
        moduleDescriptor.addFragmentProvider(DependencyKind.BINARIES, PackageFragmentProviderForMissingDependencies(moduleDescriptor))
    }
    val deserializationContext = DeserializationGlobalContext(storageManager, moduleDescriptor, classDataFinder, annotationLoader,
                                                              constantLoader, packageFragmentProvider)

    private fun createDummyPackageFragment(fqName: FqName): MutablePackageFragmentDescriptor {
        return MutablePackageFragmentDescriptor(moduleDescriptor, fqName)
    }

    // we need a "magic" way to obtain ClassId from FqName
    // the idea behind this function is that we need accurate class ids only for "neighbouring" classes (inner classes, class object, etc)
    // for all others we can build any ClassId since it will resolve to MissingDependencyErrorClassDescriptor which only stores fqName
    private fun FqName.toClassId(): ClassId {
        val segments = pathSegments()
        val packageSegmentsCount = directoryPackageFqName.pathSegments().size
        if (segments.size <= packageSegmentsCount) {
            return ClassId.topLevel(this)
        }
        val packageFqName = FqName.fromSegments(segments.subList(0, packageSegmentsCount) map { it.asString() })
        if (packageFqName == directoryPackageFqName) {
            return ClassId(packageFqName, FqNameUnsafe.fromSegments(segments.subList(packageSegmentsCount, segments.size)))
        }
        return ClassId.topLevel(this)
    }

    class object {
        private val LOG = Logger.getInstance(javaClass<DeserializerForDecompiler>())

        private object LOGGING_REPORTER: ErrorReporter {
            override fun reportLoadingError(message: String, exception: Exception?) {
                LOG.error(message, exception)
            }
            override fun reportCannotInferVisibility(descriptor: CallableMemberDescriptor) {
                LOG.error("Could not infer visibility for $descriptor")
            }
            override fun reportIncompatibleAbiVersion(kotlinClass: KotlinJvmBinaryClass, actualVersion: Int) {
                LOG.error("Incompatible ABI version for class ${kotlinClass.getClassName()}, actual version: $actualVersion")
            }
        }
    }
}
