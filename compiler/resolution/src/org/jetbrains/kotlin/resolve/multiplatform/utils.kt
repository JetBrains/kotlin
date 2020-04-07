/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.scopes.ChainedMemberScope
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered

// TODO: Klibs still need to better handle source in deserialized descriptors.
internal val DeclarationDescriptorWithSource.couldHaveASource: Boolean
    get() =
        this.source.containingFile != SourceFile.NO_SOURCE_FILE || this is DeserializedDescriptor

internal fun CallableMemberDescriptor.findNamesakesFromModule(
    module: ModuleDescriptor
): Collection<CallableMemberDescriptor> {
    val scopes = when (val containingDeclaration = containingDeclaration) {
        is PackageFragmentDescriptor -> {
            listOf(module.getPackageMemberScopeWithoutDependencies(containingDeclaration.fqName))
        }
        is ClassDescriptor -> {
            val classes = containingDeclaration.findClassifiersFromModule(module)
                .mapNotNull { if (it is TypeAliasDescriptor) it.classDescriptor else it }
                .filterIsInstance<ClassDescriptor>()
            if (this is ConstructorDescriptor) return classes.flatMap { it.constructors }

            classes.map { it.unsubstitutedMemberScope }
        }
        else -> return emptyList()
    }

    return when (this) {
        is FunctionDescriptor -> scopes.flatMap {
            it.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS) { it == name }
                .filter { it.name == name }
                .filterIsInstance<CallableMemberDescriptor>()
        }

        is PropertyDescriptor -> scopes.flatMap {
            it.getContributedDescriptors(DescriptorKindFilter.VARIABLES) { it == name }
                .filter { it.name == name }
                .filterIsInstance<CallableMemberDescriptor>()
        }

        else -> throw AssertionError("Unsupported declaration: $this")
    }
}

internal fun ClassifierDescriptorWithTypeParameters.findClassifiersFromModule(
    module: ModuleDescriptor,
    includeDependencies: Boolean = false
): Collection<ClassifierDescriptorWithTypeParameters> {
    val classId = classId ?: return emptyList()

    fun MemberScope.getAllClassifiers(name: Name): Collection<ClassifierDescriptorWithTypeParameters> =
        getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS) { it == name }
            .filterIsInstance<ClassifierDescriptorWithTypeParameters>()

    val segments = classId.relativeClassName.pathSegments()

    val scope = if (includeDependencies)
        module.getPackage(classId.packageFqName).memberScope
    else
        module.getPackageMemberScopeWithoutDependencies(classId.packageFqName)

    var classifiers = scope.getAllClassifiers(segments.first())

    for (name in segments.subList(1, segments.size)) {
        classifiers = classifiers.mapNotNull { classifier ->
            (classifier as? ClassDescriptor)?.unsubstitutedInnerClassesScope?.getContributedClassifier(
                name, NoLookupLocation.FOR_ALREADY_TRACKED
            ) as? ClassifierDescriptorWithTypeParameters
        }
    }

    return classifiers
}

internal fun ClassDescriptor.getMembers(name: Name? = null): Collection<MemberDescriptor> {
    val nameFilter = if (name != null) { it -> it == name } else MemberScope.ALL_NAME_FILTER
    return defaultType.memberScope
        .getDescriptorsFiltered(nameFilter = nameFilter)
        .filterIsInstance<MemberDescriptor>()
        .filterNot(DescriptorUtils::isEnumEntry)
        .plus(constructors.filter { nameFilter(it.name) })
}


internal fun ModuleDescriptor.getPackageMemberScopeWithoutDependencies(fqName: FqName): MemberScope {
    require(this is ModuleDescriptorImpl) { "Unexpected subtype of ModuleDescriptor: ${this::class}" }

    val memberScopes = packageFragmentProviderForModuleContentWithoutDependencies.getPackageFragments(fqName)
        .map { it.getMemberScope() }
    return ChainedMemberScope("Scope of package $fqName in module $this without dependencies", memberScopes)
}
