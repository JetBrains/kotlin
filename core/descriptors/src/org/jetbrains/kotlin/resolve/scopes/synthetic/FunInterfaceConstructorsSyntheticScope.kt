/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.scopes.synthetic

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.sam.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.storage.StorageManager

class FunInterfaceConstructorsScopeProvider(
    storageManager: StorageManager,
    lookupTracker: LookupTracker,
    samResolver: SamConversionResolver,
    samConversionOracle: SamConversionOracle
) : SyntheticScopes {
    override val scopes: Collection<SyntheticScope> = listOf(
        FunInterfaceConstructorsSyntheticScope(storageManager, lookupTracker, samResolver, samConversionOracle)
    )
}

class FunInterfaceConstructorsSyntheticScope(
    storageManager: StorageManager,
    private val lookupTracker: LookupTracker,
    private val samResolver: SamConversionResolver,
    private val samConversionOracle: SamConversionOracle
) : SyntheticScope.Default() {

    private val samConstructorForClassifier =
        storageManager.createMemoizedFunction<ClassDescriptor, SamConstructorDescriptor> { classifier ->
            createSamConstructorFunction(classifier.containingDeclaration, classifier, samResolver, samConversionOracle)
        }

    override fun getSyntheticConstructors(scope: ResolutionScope, name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        val classifier = scope.getContributedClassifier(name, location) ?: return emptyList()
        recordSamLookupsToClassifier(classifier, location)

        return listOfNotNull(getSamConstructor(classifier))
    }

    override fun getSyntheticConstructors(scope: ResolutionScope): Collection<FunctionDescriptor> {
        val classifiers = scope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS).filterIsInstance<ClassifierDescriptor>()

        return classifiers.mapNotNull { getSamConstructor(it) }
    }

    private fun getSamConstructor(classifier: ClassifierDescriptor): SamConstructorDescriptor? {
        if (classifier is TypeAliasDescriptor) {
            return getTypeAliasSamConstructor(classifier)
        }

        val classDescriptor = checkIfClassifierApplicable(classifier) ?: return null
        return samConstructorForClassifier(classDescriptor)
    }

    private fun getTypeAliasSamConstructor(classifier: TypeAliasDescriptor): SamConstructorDescriptor? {
        val classDescriptor = checkIfClassifierApplicable(classifier.classDescriptor ?: return null) ?: return null

        return createTypeAliasSamConstructorFunction(
            classifier, samConstructorForClassifier(classDescriptor), samResolver, samConversionOracle
        )
    }

    private fun checkIfClassifierApplicable(classifier: ClassifierDescriptor): ClassDescriptor? {
        if (classifier !is ClassDescriptor) return null
        if (!classifier.isFun) return null
        if (getSingleAbstractMethodOrNull(classifier) == null) return null

        return classifier
    }

    private fun recordSamLookupsToClassifier(classifier: ClassifierDescriptor, location: LookupLocation) {
        if (classifier !is ClassDescriptor || !classifier.isFun) return
        lookupTracker.record(location, classifier, SAM_LOOKUP_NAME)
    }
}