/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.expectactual

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.project.isTestModule
import org.jetbrains.kotlin.idea.caches.project.toDescriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.util.hasPrivateModifier
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection

class TypeAccessibilityCheckerImpl(
    override val project: Project,
    override val targetModule: Module,
    override var existingFqNames: Collection<String> = emptyList()
) : TypeAccessibilityChecker {
    private val scope by lazy { GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(targetModule, targetModule.isTestModule) }
    private val builtInsModule by lazy { targetModule.toDescriptor() }

    override fun incorrectTypes(declaration: KtNamedDeclaration): Collection<FqName?> =
        declaration.descriptor?.let { incorrectTypesInSequence(it.collectAllTypes(), false) } ?: listOf(null)

    override fun incorrectTypes(descriptor: DeclarationDescriptor): Collection<FqName?> =
        incorrectTypesInSequence(descriptor.collectAllTypes(), false)

    override fun incorrectTypes(type: KotlinType): Collection<FqName?> = incorrectTypesInSequence(type.collectAllTypes(), false)

    override fun checkAccessibility(declaration: KtNamedDeclaration): Boolean =
        !declaration.hasPrivateModifier() && declaration.descriptor?.let { checkAccessibility(it) } == true

    override fun checkAccessibility(descriptor: DeclarationDescriptor): Boolean =
        runInContext(descriptor.additionalClasses(existingFqNames)) {
            incorrectTypesInSequence(descriptor.collectAllTypes(), true).isEmpty()
        }

    override fun checkAccessibility(type: KotlinType): Boolean =
        incorrectTypesInSequence(type.collectAllTypes(), true).isEmpty()

    override fun <R> runInContext(fqNames: Collection<String>, block: TypeAccessibilityChecker.() -> R): R {
        val oldValue = existingFqNames
        existingFqNames = fqNames
        return block().also { existingFqNames = oldValue }
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

    private fun FqName?.canFindClassInModule(): Boolean {
        val name = this?.asString() ?: return false
        return name in existingFqNames
                || builtInsModule?.resolveClassByFqName(this, NoLookupLocation.FROM_BUILTINS) != null
                || KotlinFullClassNameIndex.getInstance()[name, project, scope].isNotEmpty()
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
        is ClassConstructorDescriptor -> valueParameters.asSequence().map(ValueParameterDescriptor::getType).flatMap(KotlinType::collectAllTypes)
        is ClassDescriptor -> if (isInline) unsubstitutedPrimaryConstructor?.collectAllTypes().orEmpty() else {
            emptySequence()
        } + declaredTypeParameters.asSequence().flatMap(DeclarationDescriptor::collectAllTypes) + sequenceOf(fqNameOrNull())
        is CallableDescriptor -> {
            val returnType = returnType ?: return sequenceOf(null)
            returnType.collectAllTypes() +
                    explicitParameters.asSequence().map(ParameterDescriptor::getType).flatMap(KotlinType::collectAllTypes) +
                    typeParameters.asSequence().flatMap(DeclarationDescriptor::collectAllTypes)
        }
        is TypeParameterDescriptor -> {
            val upperBounds = upperBounds
            if (upperBounds.isEmpty()) sequenceOf(fqNameOrNull())
            else upperBounds.asSequence().flatMap(KotlinType::collectAllTypes)
        }
        else -> emptySequence()
    }
}

private fun KotlinType.collectAllTypes(): Sequence<FqName?> = sequenceOf(fqName) + arguments.asSequence()
    .map(TypeProjection::getType)
    .flatMap(KotlinType::collectAllTypes)
