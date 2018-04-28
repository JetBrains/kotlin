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

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.backend.konan.InteropBuiltIns
import org.jetbrains.kotlin.backend.konan.serialization.KonanPackageFragment
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
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
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

/**
 * The package fragment to export forward declarations from interop package namespace, i.e.
 * redirect "$pkg.$name" to e.g. "cnames.structs.$name".
 */
class ExportedForwardDeclarationsPackageFragmentDescriptor(
        module: ModuleDescriptor, fqName: FqName, declarations: List<FqName>
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        private val nameToFqName = declarations.map { it.shortName() to it }.toMap()

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
            val declFqName = nameToFqName[name] ?: return null

            val packageView = module.getPackage(declFqName.parent())
            return packageView.memberScope.getContributedClassifier(name, location)
        }

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, " {")
            p.pushIndent()

            // TODO

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
        targets: List<KonanPackageFragment>, module: ModuleDescriptor, fqName: FqName
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        override fun getContributedClassifier(name: Name, location: LookupLocation) =
                targets.firstNotNullResult {
                    if (it.hasTopLevelClassifier(name)) {
                        it.getMemberScope().getContributedClassifier(name, location)
                    } else {
                        null
                    }
                }

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, " {")
            p.pushIndent()

            p.println("targets = " + targets)

            p.popIndent()
            p.println("}")
        }
    }

    override fun getMemberScope(): MemberScope = memberScope
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
            val descriptor = builtIns.builtInsModule.getPackage(InteropBuiltIns.FqNames.packageName)
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

/**
 * Creates module which "contains" forward declarations.
 * Note: this module should be unique per compilation and should always be the last dependency of any module.
 */
fun createForwardDeclarationsModule(builtIns: KotlinBuiltIns, storageManager: StorageManager): ModuleDescriptorImpl {
    val module = createKonanModuleDescriptor(
            Name.special("<forward declarations>"),
            storageManager,
            builtIns,
            origin = SyntheticModules
    )

    fun createPackage(fqName: FqName, supertypeName: String, classKind: ClassKind = ClassKind.CLASS) =
            ForwardDeclarationsPackageFragmentDescriptor(
                    storageManager,
                    module,
                    fqName,
                    Name.identifier(supertypeName),
                    classKind
            )

    val fqNames = InteropBuiltIns.FqNames

    val packageFragmentProvider = PackageFragmentProviderImpl(
            listOf(
                    createPackage(fqNames.cNamesStructs, "COpaque"),
                    createPackage(fqNames.objCNamesClasses, "ObjCObjectBase"),
                    createPackage(fqNames.objCNamesProtocols, "ObjCObject", ClassKind.INTERFACE)
            )
    )

    module.initialize(packageFragmentProvider)
    module.setDependencies(module)

    return module
}