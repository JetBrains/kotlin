package org.jetbrains.kotlin.library.metadata.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.exportForwardDeclarations
import org.jetbrains.kotlin.library.isInterop
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.library.packageFqName
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer

// TODO decouple and move interop-specific logic back to Kotlin/Native.
open class KlibMetadataDeserializedPackageFragmentsFactoryImpl : KlibMetadataDeserializedPackageFragmentsFactory {

    override fun createDeserializedPackageFragments(
        library: KotlinLibrary,
        packageFragmentNames: List<String>,
        moduleDescriptor: ModuleDescriptor,
        packageAccessedHandler: PackageAccessHandler?,
        storageManager: StorageManager,
        configuration: DeserializationConfiguration
    ): List<KlibMetadataDeserializedPackageFragment> {
        val libraryHeader = (packageAccessedHandler ?: SimplePackageAccessHandler).loadModuleHeader(library)

        return packageFragmentNames.flatMap {
            val fqName = FqName(it)
            val containerSource = KlibDeserializedContainerSource(library, libraryHeader, configuration, fqName)
            val parts = library.packageMetadataParts(fqName.asString())
            val isBuiltInModule = moduleDescriptor.builtIns.builtInsModule === moduleDescriptor
            parts.map { partName ->
                if (isBuiltInModule)
                    BuiltInKlibMetadataDeserializedPackageFragment(
                        fqName,
                        library,
                        packageAccessedHandler,
                        storageManager,
                        moduleDescriptor,
                        partName,
                        containerSource
                    )
                else
                    KlibMetadataDeserializedPackageFragment(
                        fqName,
                        library,
                        packageAccessedHandler,
                        storageManager,
                        moduleDescriptor,
                        partName,
                        containerSource
                    )
            }
        }
    }

    override fun createCachedPackageFragments(
        packageFragments: List<ByteArray>,
        moduleDescriptor: ModuleDescriptor,
        storageManager: StorageManager
    ) = packageFragments.map { byteArray ->
        KlibMetadataCachedPackageFragment(byteArray, storageManager, moduleDescriptor)
    }

}

/**
 * The package fragment to export forward declarations from interop package namespace, i.e.
 * redirect "$pkg.$name" to e.g. "cnames.structs.$name".
 */
@Deprecated(level = DeprecationLevel.ERROR, message = "This class is not removed for binary compatibility. It's never created now and should not be used.")
@Suppress("UNUSED_PARAMETER")
class ExportedForwardDeclarationsPackageFragmentDescriptor(
    module: ModuleDescriptor,
    fqName: FqName,
    declarations: List<FqName>
) : PackageFragmentDescriptorImpl(module, fqName) {
    override fun getMemberScope() = MemberScope.Empty
}

/**
 * The package fragment that redirects all requests for classifier lookup to its targets.
 */
@Deprecated(level = DeprecationLevel.ERROR, message = "This class is not removed for binary compatibility. It's never created now and should not be used.")
@Suppress("UNUSED_PARAMETER")
class ClassifierAliasingPackageFragmentDescriptor(
    targets: List<KlibMetadataPackageFragment>,
    module: ModuleDescriptor,
    private val checker: ExportedForwardDeclarationChecker,
) : PackageFragmentDescriptorImpl(module, checker.declKind.packageFqName) {
    override fun getMemberScope(): MemberScope = MemberScope.Empty
}

/**
 * It is possible to have different C and Objective-C declarations with the same name. That's why
 * we need to check declaration type before returning the result of lookup.
 * See KT-49034.
 */
enum class ExportedForwardDeclarationChecker(val declKind: NativeForwardDeclarationKind) {

    Struct(NativeForwardDeclarationKind.Struct),
    ObjCClass(NativeForwardDeclarationKind.ObjCClass),
    ObjCProtocol(NativeForwardDeclarationKind.ObjCProtocol)
    ;

    fun check(classifierDescriptor: ClassifierDescriptor): Boolean = classifierDescriptor is ClassDescriptor &&
            classifierDescriptor.kind == declKind.classKind &&
            classifierDescriptor.getAllSuperClassifiers().any { it.fqNameSafe == declKind.matchSuperClassFqName }
}
