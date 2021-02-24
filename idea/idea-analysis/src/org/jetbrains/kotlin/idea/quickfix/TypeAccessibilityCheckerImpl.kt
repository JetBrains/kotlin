/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.project.isTestModule
import org.jetbrains.kotlin.idea.caches.project.toDescriptor
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isNullableAny
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class TypeAccessibilityCheckerImpl(
    override val project: Project,
    override val targetModule: Module,
    override var existingTypeNames: Collection<String> = emptyList()
) : TypeAccessibilityChecker {
    private val scope by lazy { GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(targetModule, targetModule.isTestModule) }
    private var builtInsModule: ModuleDescriptor? = targetModule.toDescriptor()
        get() = if (field?.isValid != false) field
        else {
            field = targetModule.toDescriptor()
            field
        }

    override fun incorrectTypes(declaration: KtNamedDeclaration): Collection<FqName?> = declaration.descriptor?.let {
        incorrectTypesInDescriptor(it, false)
    } ?: listOf(null)

    override fun incorrectTypes(descriptor: DeclarationDescriptor): Collection<FqName?> = incorrectTypesInDescriptor(descriptor, false)

    override fun incorrectTypes(type: KotlinType): Collection<FqName?> = incorrectTypesInSequence(type.collectAllTypes(), false)

    override fun checkAccessibility(declaration: KtNamedDeclaration): Boolean =
        declaration.descriptor?.let { checkAccessibility(it) } == true

    override fun checkAccessibility(descriptor: DeclarationDescriptor): Boolean = incorrectTypesInDescriptor(descriptor, true).isEmpty()

    override fun checkAccessibility(type: KotlinType): Boolean = incorrectTypesInSequence(type.collectAllTypes(), true).isEmpty()

    override fun <R> runInContext(fqNames: Collection<String>, block: TypeAccessibilityChecker.() -> R): R {
        val oldValue = existingTypeNames
        existingTypeNames = fqNames
        return block().also { existingTypeNames = oldValue }
    }

    private fun incorrectTypesInSequence(
        sequence: Sequence<FqName?>,
        lazy: Boolean = true
    ): List<FqName?> {
        return if (lazy) {
            for (fqName in sequence) if (!fqName.canFindClassInModule()) return listOf(fqName)
            emptyList()
        } else sequence.filter { !it.canFindClassInModule() }.toList()
    }

    private fun incorrectTypesInDescriptor(descriptor: DeclarationDescriptor, lazy: Boolean) =
        runInContext(descriptor.additionalClasses(existingTypeNames)) {
            incorrectTypesInSequence(descriptor.collectAllTypes(), lazy)
        }

    private fun FqName?.canFindClassInModule(): Boolean {
        val name = this?.asString() ?: return false
        return name in existingTypeNames
                || KotlinFullClassNameIndex.getInstance()[name, project, scope].isNotEmpty()
                || builtInsModule?.resolveClassByFqName(this, NoLookupLocation.FROM_BUILTINS) != null
    }
}

private tailrec fun DeclarationDescriptor.additionalClasses(existingClasses: Collection<String> = emptySet()): Collection<String> =
    when (this) {
        is ClassifierDescriptorWithTypeParameters -> {
            val myParameters = existingClasses + declaredTypeParameters.map { it.fqNameOrNull()?.asString() ?: return emptySet() }
            val containingDeclaration = containingDeclaration
            if (isInner) containingDeclaration.additionalClasses(myParameters) else myParameters
        }
        is CallableDescriptor -> containingDeclaration.additionalClasses(existingClasses + typeParameters.map {
            it.fqNameOrNull()?.asString() ?: return emptySet()
        })
        else ->
            existingClasses
    }

private fun DeclarationDescriptor.collectAllTypes(): Sequence<FqName?> {
    return when (this) {
        is ClassConstructorDescriptor -> valueParameters.asSequence().map(ValueParameterDescriptor::getType)
            .flatMap(KotlinType::collectAllTypes)
        is ClassDescriptor -> if (isInlineClass()) unsubstitutedPrimaryConstructor?.collectAllTypes().orEmpty() else {
            emptySequence()
        } + declaredTypeParameters.asSequence().flatMap(DeclarationDescriptor::collectAllTypes) + sequenceOf(fqNameOrNull())
        is CallableDescriptor -> {
            val returnType = returnType ?: return sequenceOf(null)
            returnType.collectAllTypes() +
                    explicitParameters.map(ParameterDescriptor::getType).flatMap(KotlinType::collectAllTypes) +
                    typeParameters.asSequence().flatMap(DeclarationDescriptor::collectAllTypes)
        }
        is TypeParameterDescriptor -> {
            val upperBounds = upperBounds
            val singleUpperBound = upperBounds.singleOrNull()
            when {
                // case for unresolved type
                singleUpperBound?.isNullableAny() == true -> {
                    val extendBoundText = findPsi()?.safeAs<KtTypeParameter>()?.extendsBound?.text
                    if (extendBoundText == null || extendBoundText == "Any?") sequenceOf(singleUpperBound.fqName)
                    else sequenceOf(null)
                }
                upperBounds.isEmpty() -> sequenceOf(fqNameOrNull())
                else -> upperBounds.asSequence().flatMap(KotlinType::collectAllTypes)
            }
        }
        else -> emptySequence()
    }
}

private fun KotlinType.collectAllTypes(): Sequence<FqName?> = if (isError) sequenceOf(null)
else sequenceOf(fqName) + arguments.asSequence()
    .map(TypeProjection::getType)
    .flatMap(KotlinType::collectAllTypes)

private val CallableDescriptor.explicitParameters: Sequence<ParameterDescriptor>
    get() = valueParameters.asSequence() + dispatchReceiverParameter?.let {
        sequenceOf(it)
    }.orEmpty() + extensionReceiverParameter?.let {
        sequenceOf(it)
    }.orEmpty()
