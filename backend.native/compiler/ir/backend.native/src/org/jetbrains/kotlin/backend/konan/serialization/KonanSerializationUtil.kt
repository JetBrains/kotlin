package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanPlatform
import org.jetbrains.kotlin.backend.konan.llvm.base64Encode
import org.jetbrains.kotlin.backend.konan.llvm.base64Decode
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl;
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.AnnotationSerializer
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.utils.*
import org.jetbrains.kotlin.incremental.components.LookupTracker

/* ------------ Deserializer part ------------------------------------------*/

object NullFlexibleTypeDeserializer : FlexibleTypeDeserializer {
    override fun create(proto: ProtoBuf.Type, flexibleId: String, 
        lowerBound: SimpleType, upperBound: SimpleType): KotlinType {
            error("Illegal use of flexible type deserializer.")
        }
}

fun createKonanPackageFragmentProvider(
        fragments: List<KonanLinkData.PackageFragment>, 
        storageManager: StorageManager, module: ModuleDescriptor, 
        configuration: DeserializationConfiguration): PackageFragmentProvider {

    val packageFragments = fragments.map { 
        KonanPackageFragment(it, storageManager, module) 
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

internal fun deserializeModule(configuration: CompilerConfiguration, 
    base64: String, moduleName: String): ModuleDescriptorImpl {

    val storageManager = LockBasedStorageManager()
    val moduleDescriptor = ModuleDescriptorImpl(
            Name.special(moduleName), storageManager, KonanPlatform.builtIns)
    val deserialization_config = CompilerDeserializationConfiguration(
        configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl.DEFAULT))


    val libraryAsByteArray = base64Decode(base64)
    val libraryProto = KonanLinkData.Library.parseFrom(
        libraryAsByteArray, KonanSerializerProtocol.extensionRegistry)

    val provider = createKonanPackageFragmentProvider(
        libraryProto.packageFragmentList, storageManager, 
        moduleDescriptor, deserialization_config) 

    moduleDescriptor.initialize(provider ?: PackageFragmentProvider.Empty)

    return moduleDescriptor
}


/* ------------ Serializer part ------------------------------------------*/

internal class KonanSerializationUtil(val context: Context) {

    val serializerExtension = KonanSerializerExtension(context)
    val serializer = DescriptorSerializer.createTopLevel(serializerExtension)

    fun serializeClass(packageName: FqName,
        builder: KonanLinkData.Classes.Builder,  
        classDescriptor: ClassDescriptor, 
        skip: (DeclarationDescriptor) -> Boolean) {

        if (skip(classDescriptor)) return

        val classProto = serializer.classProto(classDescriptor).build() 
            ?: error("Class not serialized: $classDescriptor")

        builder.addClasses(classProto)
        val index = serializer.stringTable.getFqNameIndex(classDescriptor)
        builder.addClassName(index)

        serializeClasses(packageName, builder, 
            classDescriptor.unsubstitutedInnerClassesScope
                .getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS), skip)
    }

    fun serializeClasses(packageName: FqName, 
        builder: KonanLinkData.Classes.Builder, 
        descriptors: Collection<DeclarationDescriptor>, 
        skip: (DeclarationDescriptor) -> Boolean) {

        for (descriptor in descriptors) {
            if (descriptor is ClassDescriptor) {
                serializeClass(packageName, builder, descriptor, skip)
            }
        }
    }

    fun serializePackage(fqName: FqName, module: ModuleDescriptor) : 
        KonanLinkData.PackageFragment {

        val packageView = module.getPackage(fqName)

        // TODO: ModuleDescriptor should be able to return 
        // the package only with the contents of that module, without dependencies
        val skip: (DeclarationDescriptor) -> Boolean = 
            { DescriptorUtils.getContainingModule(it) != module }

        val classifierDescriptors = DescriptorSerializer.sort(packageView.memberScope.getContributedDescriptors(
                DescriptorKindFilter.CLASSIFIERS))

        val classesBuilder = KonanLinkData.Classes.newBuilder()

        serializeClasses(fqName, classesBuilder, classifierDescriptors, skip)
        val classesProto = classesBuilder.build()

        val fragments = packageView.fragments
        val members = fragments
                .flatMap { fragment -> DescriptorUtils.getAllDescriptors(fragment.getMemberScope()) }
                .filterNot(skip)
        val packageProto = serializer.packagePartProto(members).build() 
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

    internal fun serializeModule(moduleDescriptor: ModuleDescriptor): String {
        val library = KonanLinkData.Library.newBuilder()

        getPackagesFqNames(moduleDescriptor).forEach {
            val packageProto = serializePackage(it, moduleDescriptor)
            library.addPackageFragment(packageProto)
        }

        val libraryAsByteArray = library.build().toByteArray()
        val base64 = base64Encode(libraryAsByteArray)
        return base64
    }
}

