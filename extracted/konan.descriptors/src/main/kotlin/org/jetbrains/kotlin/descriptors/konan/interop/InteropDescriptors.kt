package org.jetbrains.kotlin.descriptors.konan.interop

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.SyntheticModulesOrigin
import org.jetbrains.kotlin.descriptors.konan.createKonanModuleDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.Printer

/**
 * Creates module which "contains" forward declarations.
 * Note: this module should be unique per compilation and should always be the last dependency of any module.
 */
fun createForwardDeclarationsModule(builtIns: KotlinBuiltIns, storageManager: StorageManager): ModuleDescriptorImpl {
    val module = createKonanModuleDescriptor(
            Name.special("<forward declarations>"),
            storageManager,
            builtIns,
            origin = SyntheticModulesOrigin
    )

    fun createPackage(fqName: FqName, supertypeName: String, classKind: ClassKind = ClassKind.CLASS) =
            ForwardDeclarationsPackageFragmentDescriptor(
                    storageManager,
                    module,
                    fqName,
                    Name.identifier(supertypeName),
                    classKind
            )

    val packageFragmentProvider = PackageFragmentProviderImpl(
            listOf(
                    createPackage(InteropFqNames.cNamesStructs, "COpaque"),
                    createPackage(InteropFqNames.objCNamesClasses, "ObjCObjectBase"),
                    createPackage(InteropFqNames.objCNamesProtocols, "ObjCObject", ClassKind.INTERFACE)
            )
    )

    module.initialize(packageFragmentProvider)
    module.setDependencies(module)

    return module
}

/**
 * Package fragment which creates descriptors for forward declarations on demand.
 */
private class ForwardDeclarationsPackageFragmentDescriptor(
        storageManager: StorageManager,
        module: ModuleDescriptor, fqName: FqName, supertypeName: Name, classKind: ClassKind
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        private val declarations = storageManager.createMemoizedFunction(this::createDeclaration)

        private val supertype by storageManager.createLazyValue {
            val descriptor = builtIns.builtInsModule.getPackage(InteropFqNames.packageName)
                    .memberScope
                    .getContributedClassifier(supertypeName, NoLookupLocation.FROM_BACKEND) as ClassDescriptor

            descriptor.defaultType
        }

        private fun createDeclaration(name: Name): ClassDescriptor {
            return ClassDescriptorImpl(
                    this@ForwardDeclarationsPackageFragmentDescriptor,
                    name, Modality.FINAL, classKind,
                    listOf(supertype), SourceElement.NO_SOURCE, false, LockBasedStorageManager.NO_LOCKS
            ).apply {
                this.initialize(MemberScope.Empty, emptySet(), null)
            }
        }

        override fun getContributedClassifier(name: Name, location: LookupLocation) = declarations(name)

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, "{}")
        }
    }

    override fun getMemberScope(): MemberScope = memberScope
}

object InteropFqNames {

    const val cPointerName = "CPointer"
    const val nativePointedName = "NativePointed"

    val packageName = FqName("kotlinx.cinterop")

    val cPointer = packageName.child(Name.identifier(cPointerName)).toUnsafe()
    val nativePointed = packageName.child(Name.identifier(nativePointedName)).toUnsafe()

    val cNames = FqName("cnames")
    val cNamesStructs = cNames.child(Name.identifier("structs"))

    val objCNames = FqName("objcnames")
    val objCNamesClasses = objCNames.child(Name.identifier("classes"))
    val objCNamesProtocols = objCNames.child(Name.identifier("protocols"))
}
