/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered

class DescriptorByIdSignatureFinder(
    private val moduleDescriptor: ModuleDescriptor,
    private val mangler: KotlinMangler.DescriptorMangler,
    private val lookupMode: LookupMode
) {
    init {
        assert(lookupMode != LookupMode.MODULE_ONLY || moduleDescriptor is ModuleDescriptorImpl)
    }

    /**
     * Sets search scope for [findDescriptorBySignature].
     */
    enum class LookupMode {
        /**
         * Perform search of descriptor in [moduleDescriptor] and its dependencies.
         */
        MODULE_WITH_DEPENDENCIES,

        /**
         * Perform search of descriptor only in [moduleDescriptor].
         */
        MODULE_ONLY
    }

    fun findDescriptorBySignature(signature: IdSignature): DeclarationDescriptor? = when (signature) {
        is IdSignature.AccessorSignature -> findDescriptorForAccessorSignature(signature)
        is IdSignature.PublicSignature -> findDescriptorForPublicSignature(signature)
        else -> error("only PublicSignature or AccessorSignature should reach this point, got $signature")
    }

    private fun findDescriptorForAccessorSignature(signature: IdSignature.AccessorSignature): DeclarationDescriptor? {
        val propertyDescriptor = findDescriptorBySignature(signature.propertySignature) as? PropertyDescriptor
            ?: return null
        val shortName = signature.accessorSignature.shortName
        return propertyDescriptor.accessors.singleOrNull { it.name.asString() == shortName }
    }

    private fun performLookup(signature: IdSignature.PublicSignature): Collection<DeclarationDescriptor> {
        val declarationName = signature.firstNameSegment
        return when (lookupMode) {
            LookupMode.MODULE_WITH_DEPENDENCIES -> {
                moduleDescriptor
                    .getPackage(signature.packageFqName())
                    .memberScope
                    .getContributedDescriptors { name -> name.asString() == declarationName }
            }
            LookupMode.MODULE_ONLY -> {
                (moduleDescriptor as ModuleDescriptorImpl)
                    .packageFragmentProviderForModuleContentWithoutDependencies
                    .getPackageFragments(signature.packageFqName())
                    .map { it.getMemberScope() }
                    .flatMap { it.getContributedDescriptors { name -> name.asString() == declarationName } }
            }
        }
    }

    private fun findDescriptorForPublicSignature(signature: IdSignature.PublicSignature): DeclarationDescriptor? {
        val toplevelDescriptors = performLookup(signature)
            .ifEmpty { return null }
        val candidates = signature.nameSegments.drop(1).fold(toplevelDescriptors) { acc, current ->
            acc.flatMap { container ->
                val classDescriptor = container as? ClassDescriptor ?: return@flatMap emptyList<DeclarationDescriptor>()
                classDescriptor.constructors.filter { it.name.asString() == current } +
                        classDescriptor.unsubstitutedMemberScope.getDescriptorsFiltered { name -> name.asString() == current } +
                        // Static scope is required only for Enum.values() and Enum.valueOf().
                        classDescriptor.staticScope.getDescriptorsFiltered { name -> name.asString() == current }
            }
        }

        return when (candidates.size) {
            1 -> candidates.first()
            else -> {
                findDescriptorByHash(candidates, signature.id)
                    ?: error("No descriptor found for $signature")
            }
        }
    }

    private fun findDescriptorByHash(candidates: Collection<DeclarationDescriptor>, id: Long?): DeclarationDescriptor? =
        candidates.firstOrNull { candidate ->
            if (id == null) {
                // We don't compute id for typealiases and classes.
                candidate is ClassDescriptor || candidate is TypeAliasDescriptor
            } else {
                val candidateHash = with(mangler) { candidate.signatureMangle }
                candidateHash == id
            }
        }
}
