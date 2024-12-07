/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.INVISIBLE_FAKE
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.util.DescriptorByIdSignatureFinder
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.addIfNotNull

class DescriptorByIdSignatureFinderImpl(
    private val moduleDescriptor: ModuleDescriptor,
    private val mangler: KotlinMangler.DescriptorMangler,
    private val lookupMode: LookupMode = LookupMode.MODULE_WITH_DEPENDENCIES,
) : DescriptorByIdSignatureFinder {
    init {
        assert(lookupMode != LookupMode.MODULE_ONLY || moduleDescriptor is ModuleDescriptorImpl) {
            "Incorrect lookup mode $lookupMode for $moduleDescriptor"
        }
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

    override fun findDescriptorBySignature(signature: IdSignature): DeclarationDescriptor? {
        val descriptor = when (signature) {
            is IdSignature.AccessorSignature -> resolveAccessorSignature(signature)
            is IdSignature.CommonSignature -> resolveCommonSignature(signature)
            is IdSignature.CompositeSignature -> resolveCompositeSignature(signature)
            else -> error("Unexpected signature kind: $signature")
        }

        return when (descriptor) {
            null -> null
            is PropertyAccessorDescriptor -> descriptor.takeIf { it.visibility != INVISIBLE_FAKE && it.correspondingProperty.visibility != INVISIBLE_FAKE }
            is DeclarationDescriptorWithVisibility -> descriptor.takeIf { it.visibility != INVISIBLE_FAKE }
            else -> descriptor
        }
    }

    private fun resolveCompositeSignature(signature: IdSignature.CompositeSignature): DeclarationDescriptor? {
        val container = findDescriptorBySignature(signature.nearestPublicSig()) ?: return null

        (signature.inner as? IdSignature.LocalSignature)?.let { inner ->
            fun isTypeParameterSig(fqn: String): Boolean =
                fqn == MangleConstant.TYPE_PARAMETER_MARKER_NAME || fqn == MangleConstant.TYPE_PARAMETER_MARKER_NAME_SETTER

            if (isTypeParameterSig(inner.localFqn)) {
                val tpIndex = inner.index()
                if (container is CallableDescriptor) {
                    return container.typeParameters[tpIndex]
                }
                if (container is ClassifierDescriptorWithTypeParameters) {
                    return container.declaredTypeParameters[tpIndex]
                }
            }
        }

        return container
    }

    private fun resolveAccessorSignature(signature: IdSignature.AccessorSignature): DeclarationDescriptor? {
        val propertyDescriptor = findDescriptorBySignature(signature.propertySignature) as? PropertyDescriptor ?: return null
        return when (signature.accessorSignature.shortName) {
            propertyDescriptor.getter?.name?.asString() -> propertyDescriptor.getter
            propertyDescriptor.setter?.name?.asString() -> propertyDescriptor.setter
            else -> null
        }
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

    private fun lookupTopLevelDescriptors(nameSegments: List<String>, packageFqName: FqName): Collection<DeclarationDescriptor> {
        val declarationName = nameSegments[0]
        val isLeaf = nameSegments.size == 1
        return when (lookupMode) {
            LookupMode.MODULE_WITH_DEPENDENCIES -> {
                moduleDescriptor
                    .getPackage(packageFqName)
                    .memberScope
                    .loadDescriptors(declarationName, isLeaf)
            }
            LookupMode.MODULE_ONLY -> {
                (moduleDescriptor as ModuleDescriptorImpl)
                    .packageFragmentProviderForModuleContentWithoutDependencies
                    .packageFragments(packageFqName)
                    .flatMap { it.getMemberScope().loadDescriptors(declarationName, isLeaf) }
            }
        }
    }

    private fun resolveCommonSignature(signature: IdSignature.CommonSignature): DeclarationDescriptor? {
        val nameSegments = signature.nameSegments
        val toplevelDescriptors = lookupTopLevelDescriptors(nameSegments, signature.packageFqName())
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
                        }
                        addAll(classDescriptor.staticScope.getContributedDescriptors().filter { it.name == current })
                    }
                }
            }
        }
        val candidates = acc

        return findDescriptorByHash(candidates, signature.id)
    }

    private fun findDescriptorByHash(candidates: Collection<DeclarationDescriptor>, id: Long?): DeclarationDescriptor? =
        candidates.firstOrNull { candidate ->
            if (id == null) {
                // We don't compute id for typealiases and classes.
                candidate is ClassDescriptor || candidate is TypeAliasDescriptor
            } else {
                val candidateHash = with(mangler) { candidate.signatureMangle(compatibleMode = false) }
                candidateHash == id
            }
        }
}
