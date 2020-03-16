/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler

class DescriptorByIdSignatureFinder(private val moduleDescriptor: ModuleDescriptor, private val mangler: KotlinMangler.DescriptorMangler) {
    fun findDescriptorBySignature(signature: IdSignature): DeclarationDescriptor? = when (signature) {
        is IdSignature.AccessorSignature -> findDescriptorForAccessorSignature(signature)
        is IdSignature.PublicSignature -> findDescriptorForPublicSignature(signature)
        else -> error("only PublicSignature or AccessorSignature should reach this point, got $signature")
    }

    private fun findDescriptorForAccessorSignature(signature: IdSignature.AccessorSignature): DeclarationDescriptor? {
        val propertyDescriptor = findDescriptorBySignature(signature.propertySignature) as? PropertyDescriptor
            ?: return null
        return propertyDescriptor.accessors.singleOrNull {
            it.name == signature.accessorSignature.declarationFqn.shortName()
        }
    }

    private fun findDescriptorForPublicSignature(signature: IdSignature.PublicSignature): DeclarationDescriptor? {
        val packageDescriptor = moduleDescriptor.getPackage(signature.packageFqName())
        val pathSegments = signature.declarationFqn.pathSegments()
        val toplevelDescriptors = packageDescriptor.memberScope.getContributedDescriptors { name -> name == pathSegments.first() }
            .filter { it.name == pathSegments.first() }
        val candidates = pathSegments.drop(1).fold(toplevelDescriptors) { acc, current ->
            acc.flatMap { container ->
                val classDescriptor = container as? ClassDescriptor
                    ?: return@flatMap emptyList<DeclarationDescriptor>()
                val nextStepCandidates = classDescriptor.constructors +
                        classDescriptor.unsubstitutedMemberScope.getContributedDescriptors { name -> name == current } +
                        // Static scope is required only for Enum.values() and Enum.valueOf().
                        classDescriptor.staticScope.getContributedDescriptors { name -> name == current }
                nextStepCandidates.filter { it.name == current }
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

    private fun findDescriptorByHash(candidates: List<DeclarationDescriptor>, id: Long?): DeclarationDescriptor? =
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