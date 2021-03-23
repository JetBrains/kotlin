/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.addIfNotNull

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

    fun findDescriptorBySignature(signature: IdSignature): DeclarationDescriptor? =
        when (signature) {
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

    private fun isConstructorName(n: Name) = n.isSpecial && n.asString() == "<init>"

    private fun MemberScope.loadDescriptors(name: String, isLeaf: Boolean): Collection<DeclarationDescriptor> {
        val descriptorName = Name.guessByFirstCharacter(name)
        val classifier = getContributedClassifier(descriptorName, NoLookupLocation.FROM_BACKEND)
        if (!isLeaf) {
            return listOfNotNull(classifier)
        }

        val result = mutableListOf<DeclarationDescriptor>()
        classifier?.let { result.add(it) }

        result.addAll(getContributedFunctions(descriptorName, NoLookupLocation.FROM_BACKEND))
        result.addAll(getContributedVariables(descriptorName, NoLookupLocation.FROM_BACKEND))

        return result
    }

    private fun performLookup(nameSegments: List<String>, packageFqName: FqName): Collection<DeclarationDescriptor> {
        val declarationName = nameSegments[0]
        val isLeaf = nameSegments.size == 1
        return when (lookupMode) {
            LookupMode.MODULE_WITH_DEPENDENCIES -> {
                moduleDescriptor
                    .getPackage(packageFqName)
                    .memberScope.loadDescriptors(declarationName, isLeaf)
            }
            LookupMode.MODULE_ONLY -> {
                (moduleDescriptor as ModuleDescriptorImpl)
                    .packageFragmentProviderForModuleContentWithoutDependencies
                    .packageFragments(packageFqName)
                    .flatMap { it.getMemberScope().loadDescriptors(declarationName, isLeaf) }
            }
        }
    }

    private fun findDescriptorForPublicSignature(signature: IdSignature.PublicSignature): DeclarationDescriptor? {
        val nameSegments = signature.nameSegments
        val toplevelDescriptors = performLookup(nameSegments, signature.packageFqName())
            .ifEmpty { return null }

        var acc = toplevelDescriptors
        val lastIndex = nameSegments.lastIndex

        // The code bellow could look tricky because of it is bottle neck so here is put some attempt including
        // 1. Minimize amount of descriptors is loaded on each step
        // 2. Reduce memory pollution
        for (i in 1 until nameSegments.size) {
            val current = Name.guessByFirstCharacter(nameSegments[i])
            acc = acc.flatMap { container ->
                val classDescriptor = container as? ClassDescriptor ?: return@flatMap emptyList<DeclarationDescriptor>()
                val isLeaf = i == lastIndex
                val memberScope = classDescriptor.unsubstitutedMemberScope

                val classifier = memberScope.getContributedClassifier(current, NoLookupLocation.FROM_BACKEND)
                if (!isLeaf) {
                    classifier?.let { listOf(it) } ?: emptyList()
                } else {
                    mutableListOf<DeclarationDescriptor>().apply {
                        addIfNotNull(classifier)
                        if (signature.id != null) {
                            if (isConstructorName(current)) addAll(classDescriptor.constructors)
                            addAll(memberScope.getContributedFunctions(current, NoLookupLocation.FROM_BACKEND))
                            addAll(memberScope.getContributedVariables(current, NoLookupLocation.FROM_BACKEND))
                            addAll(classDescriptor.staticScope.getContributedDescriptors { it == current })
                        }
                    }
                }
            }
        }
        val candidates = acc

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
                val candidateHash = with(mangler) { candidate.signatureMangle() }
                candidateHash == id
            }
        }
}
