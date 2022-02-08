package org.jetbrains.kotlin.backend.common.serialization.metadata.impl

import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataDeserializedPackageFragmentsFactory
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.exportForwardDeclarations
import org.jetbrains.kotlin.library.isInterop
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.library.packageFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.serialization.konan.impl.ForwardDeclarationsFqNames
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer

// TODO decouple and move interop-specific logic back to Kotlin/Native.
open class KlibMetadataDeserializedPackageFragmentsFactoryImpl : KlibMetadataDeserializedPackageFragmentsFactory {

    override fun createDeserializedPackageFragments(
        library: KotlinLibrary,
        packageFragmentNames: List<String>,
        moduleDescriptor: ModuleDescriptor,
        packageAccessedHandler: PackageAccessHandler?,
        storageManager: StorageManager
    ) = packageFragmentNames.flatMap {
        val fqName = FqName(it)
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
                    partName
                ) else
                KlibMetadataDeserializedPackageFragment(fqName, library, packageAccessedHandler, storageManager, moduleDescriptor, partName)
        }
    }

    override fun createCachedPackageFragments(
        packageFragments: List<ByteArray>,
        moduleDescriptor: ModuleDescriptor,
        storageManager: StorageManager
    ) = packageFragments.map { byteArray ->
        KlibMetadataCachedPackageFragment(byteArray, storageManager, moduleDescriptor)
    }

    override fun createSyntheticPackageFragments(
        library: KotlinLibrary,
        deserializedPackageFragments: List<KlibMetadataPackageFragment>,
        moduleDescriptor: ModuleDescriptor
    ): List<PackageFragmentDescriptor> {

        if (!library.isInterop) return emptyList()

        val mainPackageFqName = library.packageFqName?. let{ FqName(it) }
            ?: error("Inconsistent manifest: interop library ${library.libraryName} should have `package` specified")
        val exportForwardDeclarations = library.exportForwardDeclarations.map{ FqName(it) }

        val aliasedPackageFragments = deserializedPackageFragments.filter { it.fqName == mainPackageFqName }

        val result = mutableListOf<PackageFragmentDescriptor>()
        listOf(
            ForwardDeclarationsFqNames.cNamesStructs,
            ForwardDeclarationsFqNames.objCNamesClasses,
            ForwardDeclarationsFqNames.objCNamesProtocols
        ).mapTo(result) { fqName ->
            ClassifierAliasingPackageFragmentDescriptor(aliasedPackageFragments, moduleDescriptor, fqName)
        }

        result.add(ExportedForwardDeclarationsPackageFragmentDescriptor(moduleDescriptor, mainPackageFqName, exportForwardDeclarations))

        return result
    }

}

/**
 * The package fragment to export forward declarations from interop package namespace, i.e.
 * redirect "$pkg.$name" to e.g. "cnames.structs.$name".
 */
class ExportedForwardDeclarationsPackageFragmentDescriptor(
    module: ModuleDescriptor,
    fqName: FqName,
    declarations: List<FqName>
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        private val nameToFqName = declarations.map { it.shortName() to it }.toMap()

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
            val declFqName = nameToFqName[name] ?: return null

            val packageView = module.getPackage(declFqName.parent())
            return packageView.memberScope.getContributedClassifier(name, location) // ?: FIXME(ddol): delegate to forward declarations synthetic module!
        }

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, " {")
            p.pushIndent()

            p.println("declarations = $declarations")

            p.popIndent()
            p.println("}")
        }

    }

    override fun getMemberScope() = memberScope
}

/**
 * The package fragment that redirects all requests for classifier lookup to its targets.
 */
class ClassifierAliasingPackageFragmentDescriptor(
    targets: List<KlibMetadataPackageFragment>,
    module: ModuleDescriptor,
    fqName: FqName
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        override fun getContributedClassifier(name: Name, location: LookupLocation) =
            targets.firstNotNullOfOrNull {
                if (it.hasTopLevelClassifier(name)) {
                    it.getMemberScope().getContributedClassifier(name, location)
                } else {
                    null
                }
            }

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, " {")
            p.pushIndent()

            p.println("targets = $targets")

            p.popIndent()
            p.println("}")
        }
    }

    override fun getMemberScope(): MemberScope = memberScope
}

