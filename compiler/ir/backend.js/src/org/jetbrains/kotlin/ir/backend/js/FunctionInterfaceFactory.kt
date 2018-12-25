/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.builtins.FunctionInterfacePackageFragment
import org.jetbrains.kotlin.builtins.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.BuiltInFictitiousFunctionClassFactory
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
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
        classDescriptorFactory.shouldCreateClass(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME, name) ->
            classDescriptorFactory.createClass(ClassId.topLevel(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.child(name)))

        classDescriptorFactory.shouldCreateClass(KOTLIN_REFLECT_FQ_NAME, name) ->
            classDescriptorFactory.createClass(ClassId.topLevel(KOTLIN_REFLECT_FQ_NAME.child(name)))

        else -> null
    }
}

class FunctionInterfacePackageFragmentImpl(
    classDescriptorFactory: ClassDescriptorFactory,
    private val containingModule: ModuleDescriptor
) : FunctionInterfacePackageFragment {

    private val memberScopeObj = FunctionInterfaceMemberScope(classDescriptorFactory, KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME)

    override val fqName: FqName
        get() = KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME

    private val shortName = fqName.shortName()

    override fun getName(): Name = shortName

    override fun getContainingDeclaration(): ModuleDescriptor = containingModule

    override fun getOriginal(): DeclarationDescriptorWithSource = this
    override fun getSource(): SourceElement = SourceElement.NO_SOURCE
    override val annotations: Annotations = Annotations.EMPTY

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitPackageFragmentDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitPackageFragmentDescriptor(this, null)
    }

    override fun getMemberScope() = memberScopeObj
}

class FunctionInterfacePackageFragmentProvider(
    classFactory: BuiltInFictitiousFunctionClassFactory,
    module: ModuleDescriptor
) : PackageFragmentProvider {
    private val packageFragment = FunctionInterfacePackageFragmentImpl(classFactory, module)

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        if (fqName != KOTLIN_REFLECT_FQ_NAME && fqName != KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME)
            return emptyList()
        return listOf(packageFragment)
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> =
        emptyList()
}
