/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.LinkData
import org.jetbrains.kotlin.backend.konan.KonanBuiltIns
import org.jetbrains.kotlin.backend.konan.llvm.base64Decode
import org.jetbrains.kotlin.backend.konan.llvm.base64Encode
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import org.jetbrains.kotlin.descriptors.Modality.FINAL
import org.jetbrains.kotlin.descriptors.SourceElement.NO_SOURCE
import org.jetbrains.kotlin.descriptors.Visibilities.INTERNAL
import org.jetbrains.kotlin.descriptors.annotations.Annotations.Companion.EMPTY
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.KonanDescriptorSerializer
import org.jetbrains.kotlin.serialization.KonanIr
import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

typealias Base64 = String

fun byteArrayToBase64(byteArray: ByteArray): Base64 {
    val gzipped = ByteArrayOutputStream()
    val gzipStream = GZIPOutputStream(gzipped)
    gzipStream.write(byteArray)
    gzipStream.close()
    val base64 = base64Encode(gzipped.toByteArray())
    return base64
}

fun base64ToStream(base64: Base64): InputStream {
    val gzipped = base64Decode(base64)
    return GZIPInputStream(ByteArrayInputStream(gzipped))

}


/* ------------ Deserializer part ------------------------------------------*/

object NullFlexibleTypeDeserializer : FlexibleTypeDeserializer {
    override fun create(proto: ProtoBuf.Type, flexibleId: String, 
        lowerBound: SimpleType, upperBound: SimpleType): KotlinType {
            error("Illegal use of flexible type deserializer.")
        }
}

fun createKonanPackageFragmentProvider(
        fragmentNames: List<String>, 
        packageLoader: (String)->KonanLinkData.PackageFragment,
        storageManager: StorageManager, module: ModuleDescriptor, 
        configuration: DeserializationConfiguration): PackageFragmentProvider {

    val packageFragments = fragmentNames.map{ 
        KonanPackageFragment(it, packageLoader, storageManager, module) 
    }
    val provider = PackageFragmentProviderImpl(packageFragments)

    val notFoundClasses = NotFoundClasses(storageManager, module)

    val annotationAndConstantLoader = AnnotationAndConstantLoaderImpl(module, notFoundClasses, KonanSerializerProtocol)

    val components = DeserializationComponents(
        storageManager, module, configuration, 
        DeserializedClassDataFinder(provider),
        annotationAndConstantLoader,
        provider, 
        LocalClassifierTypeSettings.Default, 
        ErrorReporter.DO_NOTHING,
        LookupTracker.DO_NOTHING, NullFlexibleTypeDeserializer,
        emptyList(), notFoundClasses)

        for (packageFragment in packageFragments) {
            packageFragment.components = components
        }

    return provider
}

internal fun deserializePackageFragment(base64: Base64): KonanLinkData.PackageFragment {
    return KonanLinkData.PackageFragment
        .parseFrom(base64ToStream(base64), 
            KonanSerializerProtocol.extensionRegistry)
}

internal fun deserializeModule(configuration: CompilerConfiguration, 
    packageLoader:(String)->Base64, library: Base64,  moduleName: String): ModuleDescriptorImpl {

    val storageManager = LockBasedStorageManager()
    val builtIns = KonanBuiltIns(storageManager)
    val moduleDescriptor = ModuleDescriptorImpl(
            Name.special(moduleName), storageManager, builtIns)
    builtIns.builtInsModule = moduleDescriptor
    val deserializationConfiguration = CompilerDeserializationConfiguration(configuration.languageVersionSettings)

    val libraryProto = KonanLinkData.Library
        .parseFrom(base64ToStream(library), 
            KonanSerializerProtocol.extensionRegistry)

    val provider = createKonanPackageFragmentProvider(
        libraryProto.packageFragmentNameList,
        {it -> deserializePackageFragment(packageLoader(it))},
        storageManager, 
        moduleDescriptor, deserializationConfiguration)

    moduleDescriptor.initialize(provider)

    return moduleDescriptor
}


/* ------------ Serializer part ------------------------------------------*/

internal class KonanSerializationUtil(val context: Context) {

    val serializerExtension = KonanSerializerExtension(context, this)
    val serializer = KonanDescriptorSerializer.createTopLevel(serializerExtension)
    val typeSerializer: (KotlinType)->Int = { it -> serializer.typeId(it) }

    fun serializeClass(packageName: FqName,
        builder: KonanLinkData.Classes.Builder,  
        classDescriptor: ClassDescriptor) {

        val localSerializer = KonanDescriptorSerializer.create(classDescriptor, serializerExtension)
        val classProto = localSerializer.classProto(classDescriptor).build()
            ?: error("Class not serialized: $classDescriptor")

        builder.addClasses(classProto)
        val index = localSerializer.stringTable.getFqNameIndex(classDescriptor)
        builder.addClassName(index)

        serializeClasses(packageName, builder, 
            classDescriptor.unsubstitutedInnerClassesScope
                .getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS))
    }

    fun serializeClasses(packageName: FqName, 
        builder: KonanLinkData.Classes.Builder, 
        descriptors: Collection<DeclarationDescriptor>) {

        for (descriptor in descriptors) {
            if (descriptor is ClassDescriptor) {
                serializeClass(packageName, builder, descriptor)
            }
        }
    }

    fun serializePackage(fqName: FqName, module: ModuleDescriptor) : 
        KonanLinkData.PackageFragment? {

        val packageView = module.getPackage(fqName)

        // TODO: ModuleDescriptor should be able to return 
        // the package only with the contents of that module, without dependencies
        val skip: (DeclarationDescriptor) -> Boolean = 
            { DescriptorUtils.getContainingModule(it) != module }

        val classifierDescriptors = KonanDescriptorSerializer
            .sort(packageView.memberScope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS))
            .filterNot(skip)

        val fragments = packageView.fragments
        val members = fragments
                .flatMap { fragment -> DescriptorUtils.getAllDescriptors(fragment.getMemberScope()) }
                .filterNot(skip)

        if (members.isEmpty() && classifierDescriptors.isEmpty()) return null

        val classesBuilder = KonanLinkData.Classes.newBuilder()

        serializeClasses(fqName, classesBuilder, classifierDescriptors)
        val classesProto = classesBuilder.build()

        val packageProto = serializer.packagePartProto(fqName, members).build()
            ?: error("Package fragments not serialized: $fragments")

        val strings = serializerExtension.stringTable
        val (stringTableProto, nameTableProto) = strings.buildProto()

        val fragmentBuilder = KonanLinkData.PackageFragment.newBuilder()

        val fragmentProto = fragmentBuilder
            .setPackage(packageProto)
            .setFqName(fqName.asString())
            .setClasses(classesProto)
            .setStringTable(stringTableProto)
            .setNameTable(nameTableProto)
            .build()

        return fragmentProto
    }

    private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
        val fqNames = mutableSetOf<FqName>(FqName.ROOT)
        getSubPackagesFqNames(module.getPackage(FqName.ROOT), fqNames)
        return fqNames
    }
    private fun getSubPackagesFqNames(packageView: PackageViewDescriptor, result: MutableSet<FqName>) {
        val fqName = packageView.fqName
        if (!fqName.isRoot) {
            result.add(fqName)
        }

        for (descriptor in packageView.memberScope.getContributedDescriptors(
                DescriptorKindFilter.PACKAGES, MemberScope.ALL_NAME_FILTER)) {
            if (descriptor is PackageViewDescriptor) {
                getSubPackagesFqNames(descriptor, result)
            }
        }
    }
    internal fun serializeModule(moduleDescriptor: ModuleDescriptor): LinkData {
        val libraryProto = KonanLinkData.Library.newBuilder()
        val fragments = mutableListOf<String>()
        val fragmentNames = mutableListOf<String>()

        getPackagesFqNames(moduleDescriptor).forEach iteration@ {
            val packageProto = serializePackage(it, moduleDescriptor)
            if (packageProto == null) return@iteration

            libraryProto.addPackageFragmentName(it.asString())
            fragments.add(
                byteArrayToBase64(packageProto.toByteArray()))
            fragmentNames.add(it.asString())
        }
        val libraryAsByteArray = libraryProto.build().toByteArray()
        val library = byteArrayToBase64(libraryAsByteArray)
        return LinkData(library, fragments, fragmentNames)
    }

    /* The section below is specific for IR serialization */

    // TODO: We utilize DescriptorSerializer's property 
    // serialization to serialize variables for now.
    // Need to negotiate the ability to deserialize 
    // variables with the big kotlin somehow.
    fun variableAsProperty(variable: VariableDescriptor): PropertyDescriptor {

        val isDelegated = when (variable) {
            is LocalVariableDescriptor -> variable.isDelegated
            is IrTemporaryVariableDescriptor -> false
            else -> error("Unexpected variable descriptor.")
        }

        val property = PropertyDescriptorImpl.create(
                variable.containingDeclaration,
                EMPTY,
                FINAL,
                INTERNAL,
                variable.isVar(),
                variable.name,
                DECLARATION,
                NO_SOURCE,
                false, false, false, false, false, 
                isDelegated)

        property.setType(variable.type, listOf(), null, null as KotlinType?)

        // TODO: transform the getter and the setter too.
        property.initialize(null, null)
        return property
    }

    fun serializeLocalDeclaration(descriptor: DeclarationDescriptor): KonanIr.DeclarationDescriptor {

        val proto = KonanIr.DeclarationDescriptor.newBuilder()

        context.log{"### serializeLocalDeclaration: $descriptor"}

        when (descriptor) {
            is FunctionDescriptor ->
                proto.setFunction(serializer.functionProto(descriptor))

            is PropertyDescriptor ->
                proto.setProperty(serializer.propertyProto(descriptor))

            is ClassDescriptor ->
                proto.setClazz(serializer.classProto(descriptor))

            is VariableDescriptor -> {
                val property = variableAsProperty(descriptor)
                serializerExtension.originalVariables.put(property, descriptor)
                proto.setProperty(serializer.propertyProto(property))
            }

            else -> error("Unexpected descriptor kind: $descriptor")
         }

         return proto.build()
     }



}

