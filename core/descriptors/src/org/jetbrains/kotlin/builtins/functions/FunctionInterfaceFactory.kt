/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.functions

import org.jetbrains.kotlin.builtins.FunctionInterfacePackageFragment
import org.jetbrains.kotlin.builtins.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME_RELEASE
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer

class FunctionInterfaceMemberScope(
    private val classDescriptorFactory: ClassDescriptorFactory,
    val packageName: FqName
) : MemberScopeImpl() {

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ) =
        classDescriptorFactory.getAllContributedClassesIfPossible(packageName)

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        emptyList()

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> =
        emptyList()

    override fun getFunctionNames(): Set<Name> =
        emptySet()

    override fun getVariableNames(): Set<Name> =
        emptySet()

    override fun getClassifierNames(): Set<Name>? = null

    override fun printScopeStructure(p: Printer) {
        TODO()
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = when {
        classDescriptorFactory.shouldCreateClass(packageName, name) ->
            classDescriptorFactory.createClass(ClassId.topLevel(packageName.child(name)))
        else -> null
    }
}

class FunctionInterfacePackageFragmentImpl(
    classDescriptorFactory: ClassDescriptorFactory,
    module: ModuleDescriptor,
    name: FqName
) : FunctionInterfacePackageFragment,
    PackageFragmentDescriptorImpl(module, name) {
    private val memberScope = FunctionInterfaceMemberScope(classDescriptorFactory, fqName)
    override fun getMemberScope() = memberScope
}

fun functionInterfacePackageFragmentProvider(
    storageManager: StorageManager,
    module: ModuleDescriptor
): PackageFragmentProvider {
    val classFactory = BuiltInFictitiousFunctionClassFactory(storageManager, module)
    val fragments = listOf(
        KOTLIN_REFLECT_FQ_NAME,
        KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME,
        COROUTINES_PACKAGE_FQ_NAME_RELEASE
    ).map { fqName ->
        FunctionInterfacePackageFragmentImpl(classFactory, module, fqName)
    }
    return PackageFragmentProviderImpl(fragments)
}
