/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.synthetic

import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.synthetic.FunctionInterfaceAdapterExtensionFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.load.java.descriptors.JavaClassConstructorDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.sam.JavaSingleAbstractMethodUtils
import org.jetbrains.kotlin.load.java.sam.SamAdapterDescriptor
import org.jetbrains.kotlin.load.java.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.inference.wrapWithCapturingSubstitution
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.sam.SAM_LOOKUP_NAME
import org.jetbrains.kotlin.resolve.sam.SamConversionOracle
import org.jetbrains.kotlin.resolve.sam.SamConversionResolver
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.findCorrespondingSupertype
import kotlin.properties.Delegates

interface SamAdapterExtensionFunctionDescriptor : FunctionDescriptor, FunctionInterfaceAdapterExtensionFunctionDescriptor {
    override val baseDescriptorForSynthetic: FunctionDescriptor
}

class SamAdapterFunctionsScope(
    storageManager: StorageManager,
    private val samResolver: SamConversionResolver,
    private val samConversionOracle: SamConversionOracle,
    private val deprecationResolver: DeprecationResolver,
    private val lookupTracker: LookupTracker,
    private val samViaSyntheticScopeDisabled: Boolean,
    private val shouldGenerateCandidateForVarargAfterSam: Boolean
) : SyntheticScope.Default() {
    private val shouldGenerateAdditionalSamCandidate = !samViaSyntheticScopeDisabled || shouldGenerateCandidateForVarargAfterSam

    private val extensionForFunction =
        storageManager.createMemoizedFunctionWithNullableValues<FunctionDescriptor, FunctionDescriptor> { function ->
            extensionForFunctionNotCached(function)
        }

    private val samAdapterForStaticFunction =
        storageManager.createMemoizedFunction<JavaMethodDescriptor, SamAdapterDescriptor<JavaMethodDescriptor>> { function ->
            JavaSingleAbstractMethodUtils
                .createSamAdapterFunction(function, samResolver, samConversionOracle)
        }

    private val samConstructorForClassifier =
        storageManager.createMemoizedFunction<ClassDescriptor, SamConstructorDescriptor> { classifier ->
            JavaSingleAbstractMethodUtils
                .createSamConstructorFunction(classifier.containingDeclaration, classifier, samResolver, samConversionOracle)
        }

    private val samConstructorForJavaConstructor =
        storageManager.createMemoizedFunction<JavaClassConstructorDescriptor, ClassConstructorDescriptor> { constructor ->
            JavaSingleAbstractMethodUtils
                .createSamAdapterConstructor(constructor, samResolver, samConversionOracle) as ClassConstructorDescriptor
        }

    private val samConstructorForTypeAliasConstructor =
        storageManager.createMemoizedFunctionWithNullableValues<Pair<ClassConstructorDescriptor, TypeAliasDescriptor>, TypeAliasConstructorDescriptor> { (constructor, typeAliasDescriptor) ->
            TypeAliasConstructorDescriptorImpl.createIfAvailable(storageManager, typeAliasDescriptor, constructor)
        }

    private fun extensionForFunctionNotCached(function: FunctionDescriptor): FunctionDescriptor? {
        if (!function.visibility.isVisibleOutside()) return null
        if (!function.hasJavaOriginInHierarchy()) return null //TODO: should we go into base at all?
        if (!JavaSingleAbstractMethodUtils.isSamAdapterNecessary(function)) return null
        if (function.returnType == null) return null
        if (deprecationResolver.isHiddenInResolution(function)) return null
        return SamAdapterExtensionFunctionDescriptorImpl.create(function, samResolver, samConversionOracle)
    }

    override fun getSyntheticMemberFunctions(
        receiverTypes: Collection<KotlinType>,
        name: Name,
        location: LookupLocation
    ): Collection<FunctionDescriptor> {
        if (!shouldGenerateAdditionalSamCandidate) return emptyList()

        var result: SmartList<FunctionDescriptor>? = null
        for (type in receiverTypes) {
            for (function in type.memberScope.getContributedFunctions(name, location)) {
                if (samViaSyntheticScopeDisabled && !function.shouldGenerateCandidateForVarargAfterSamAndHasVararg) continue

                val extension = extensionForFunction(function.original)?.substituteForReceiverType(type)
                if (extension != null) {
                    recordSamLookupsForParameters(function, location)
                    if (result == null) {
                        result = SmartList()
                    }
                    result.add(extension)
                }
            }
        }
        return when {
            result == null -> emptyList()
            result.size > 1 -> result.toSet()
            else -> result
        }
    }

    private fun recordSamLookupsForParameters(function: FunctionDescriptor, location: LookupLocation) {
        for (valueParameter in function.valueParameters) {
            recordSamLookupsToClassifier(valueParameter.type.constructor.declarationDescriptor ?: continue, location)
        }
    }

    private val FunctionDescriptor.shouldGenerateCandidateForVarargAfterSamAndHasVararg
        get() = shouldGenerateCandidateForVarargAfterSam && valueParameters.lastOrNull()?.isVararg == true

    private fun FunctionDescriptor.substituteForReceiverType(receiverType: KotlinType): FunctionDescriptor? {
        val containingClass = containingDeclaration as? ClassDescriptor ?: return null
        val correspondingSupertype = findCorrespondingSupertype(receiverType, containingClass.defaultType) ?: return null

        return substitute(
            TypeConstructorSubstitution
                .create(correspondingSupertype)
                .wrapWithCapturingSubstitution(needApproximation = true)
                .buildSubstitutor()
        )
    }

    override fun getSyntheticMemberFunctions(receiverTypes: Collection<KotlinType>): Collection<FunctionDescriptor> {
        if (!shouldGenerateAdditionalSamCandidate) return emptyList()

        return receiverTypes.flatMapTo(LinkedHashSet<FunctionDescriptor>()) { type ->
            type.memberScope.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
                .filterIsInstance<FunctionDescriptor>()
                .run {
                    if (samViaSyntheticScopeDisabled) filter { it.shouldGenerateCandidateForVarargAfterSamAndHasVararg } else this
                }
                .mapNotNull {
                    extensionForFunction(it.original)?.substituteForReceiverType(type)
                }
        }
    }

    override fun getSyntheticStaticFunctions(scope: ResolutionScope, name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        if (!shouldGenerateAdditionalSamCandidate) return emptyList()

        return getSamFunctions(scope.getContributedFunctions(name, location), location)
    }

    override fun getSyntheticConstructors(scope: ResolutionScope, name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        val classifier = scope.getContributedClassifier(name, location) ?: return emptyList()
        recordSamLookupsToClassifier(classifier, location)

        if (!shouldGenerateAdditionalSamCandidate) return listOfNotNull(getSamConstructor(classifier))

        return getAllSamConstructors(classifier)
    }

    private fun recordSamLookupsToClassifier(classifier: ClassifierDescriptor, location: LookupLocation) {
        if (classifier !is JavaClassDescriptor || classifier.kind != ClassKind.INTERFACE) return
        // TODO: We should also record SAM lookups even when the interface is not SAM
        if (!JavaSingleAbstractMethodUtils.isSamType(classifier.defaultType)) return

        lookupTracker.record(location, classifier, SAM_LOOKUP_NAME)
    }

    override fun getSyntheticStaticFunctions(scope: ResolutionScope): Collection<FunctionDescriptor> {
        if (!shouldGenerateAdditionalSamCandidate) return emptyList()

        return getSamFunctions(scope.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS), location = null)
    }

    override fun getSyntheticConstructors(scope: ResolutionScope): Collection<FunctionDescriptor> {
        val classifiers = scope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS).filterIsInstance<ClassifierDescriptor>()

        if (!shouldGenerateAdditionalSamCandidate) return classifiers.mapNotNull { getSamConstructor(it) }

        return classifiers.flatMap { getAllSamConstructors(it) }
    }

    override fun getSyntheticConstructor(constructor: ConstructorDescriptor): ConstructorDescriptor? {
        if (samViaSyntheticScopeDisabled && !constructor.shouldGenerateCandidateForVarargAfterSamAndHasVararg) return null

        return when (constructor) {
            is JavaClassConstructorDescriptor -> createJavaSamAdapterConstructor(constructor)
            is TypeAliasConstructorDescriptor -> {
                val underlyingConstructor = constructor.underlyingConstructorDescriptor
                if (underlyingConstructor !is JavaClassConstructorDescriptor) return null
                val underlyingSamConstructor = createJavaSamAdapterConstructor(underlyingConstructor) ?: return null

                samConstructorForTypeAliasConstructor(Pair(underlyingSamConstructor, constructor.typeAliasDescriptor))
            }
            else -> null
        }
    }

    private fun createJavaSamAdapterConstructor(constructor: JavaClassConstructorDescriptor): ClassConstructorDescriptor? {
        if (!JavaSingleAbstractMethodUtils.isSamAdapterNecessary(constructor)) return null
        return samConstructorForJavaConstructor(constructor)
    }

    private fun getSamFunctions(
        functions: Collection<DeclarationDescriptor>,
        location: LookupLocation?
    ): List<SamAdapterDescriptor<JavaMethodDescriptor>> {
        if (!shouldGenerateAdditionalSamCandidate) return emptyList()

        return functions.mapNotNull { function ->
            if (function !is JavaMethodDescriptor) return@mapNotNull null
            if (function.dispatchReceiverParameter != null) return@mapNotNull null // consider only statics
            if (!JavaSingleAbstractMethodUtils.isSamAdapterNecessary(function)) return@mapNotNull null
            if (samViaSyntheticScopeDisabled && !function.shouldGenerateCandidateForVarargAfterSamAndHasVararg)
                return@mapNotNull null

            location?.let { recordSamLookupsForParameters(function, it) }

            samAdapterForStaticFunction(function)
        }
    }

    private fun getAllSamConstructors(classifier: ClassifierDescriptor): List<FunctionDescriptor> {
        return getSamAdaptersFromConstructors(classifier) + listOfNotNull(getSamConstructor(classifier))
    }

    private fun getSamAdaptersFromConstructors(classifier: ClassifierDescriptor): List<FunctionDescriptor> {
        if (!shouldGenerateAdditionalSamCandidate || classifier !is JavaClassDescriptor) return emptyList()

        return arrayListOf<FunctionDescriptor>().apply {
            for (constructor in classifier.constructors) {
                val samConstructor = getSyntheticConstructor(constructor) ?: continue
                add(samConstructor)
            }
        }
    }

    private fun getSamConstructor(classifier: ClassifierDescriptor): SamConstructorDescriptor? {
        if (classifier is TypeAliasDescriptor) {
            return getTypeAliasSamConstructor(classifier)
        }

        if (classifier !is ClassDescriptor) return null
        if (!JavaSingleAbstractMethodUtils.isSamClassDescriptor(classifier)) return null

        return samConstructorForClassifier(classifier)
    }

    private fun getTypeAliasSamConstructor(classifier: TypeAliasDescriptor): SamConstructorDescriptor? {
        val classDescriptor = classifier.classDescriptor ?: return null
        if (!JavaSingleAbstractMethodUtils.isSamClassDescriptor(classDescriptor)) return null

        return JavaSingleAbstractMethodUtils.createTypeAliasSamConstructorFunction(
            classifier, samConstructorForClassifier(classDescriptor), samResolver, samConversionOracle
        )
    }

    private class SamAdapterExtensionFunctionDescriptorImpl(
        containingDeclaration: DeclarationDescriptor,
        original: SimpleFunctionDescriptor?,
        annotations: Annotations,
        name: Name,
        kind: CallableMemberDescriptor.Kind,
        source: SourceElement
    ) : SamAdapterExtensionFunctionDescriptor,
        SimpleFunctionDescriptorImpl(containingDeclaration, original, annotations, name, kind, source) {

        override var baseDescriptorForSynthetic: FunctionDescriptor by Delegates.notNull()
            private set

        private val fromSourceFunctionTypeParameters: Map<TypeParameterDescriptor, TypeParameterDescriptor> by lazy {
            baseDescriptorForSynthetic.typeParameters.zip(typeParameters).toMap()
        }

        companion object {
            fun create(
                sourceFunction: FunctionDescriptor,
                samResolver: SamConversionResolver,
                samConversionOracle: SamConversionOracle
            ): SamAdapterExtensionFunctionDescriptorImpl {
                val descriptor = SamAdapterExtensionFunctionDescriptorImpl(
                    sourceFunction.containingDeclaration,
                    null,
                    sourceFunction.annotations,
                    sourceFunction.name,
                    CallableMemberDescriptor.Kind.SYNTHESIZED,
                    sourceFunction.original.source
                )
                descriptor.baseDescriptorForSynthetic = sourceFunction

                val sourceTypeParams = (sourceFunction.typeParameters).toMutableList()
                val ownerClass = sourceFunction.containingDeclaration as ClassDescriptor

                val typeParameters = ArrayList<TypeParameterDescriptor>(sourceTypeParams.size)
                val typeSubstitutor =
                    DescriptorSubstitutor.substituteTypeParameters(sourceTypeParams, TypeSubstitution.EMPTY, descriptor, typeParameters)

                val returnType = typeSubstitutor.safeSubstitute(sourceFunction.returnType!!, Variance.INVARIANT)
                val valueParameters = JavaSingleAbstractMethodUtils.createValueParametersForSamAdapter(
                    sourceFunction, descriptor, typeSubstitutor, samResolver, samConversionOracle
                )

                val visibility = syntheticVisibility(sourceFunction, isUsedForExtension = false)

                descriptor.initialize(
                    null, ownerClass.thisAsReceiverParameter, typeParameters, valueParameters, returnType,
                    Modality.FINAL, visibility
                )

                descriptor.isOperator = sourceFunction.isOperator
                descriptor.isInfix = sourceFunction.isInfix

                return descriptor
            }
        }

        override fun hasStableParameterNames() = baseDescriptorForSynthetic.hasStableParameterNames()
        override fun hasSynthesizedParameterNames() = baseDescriptorForSynthetic.hasSynthesizedParameterNames()

        override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            original: FunctionDescriptor?,
            kind: CallableMemberDescriptor.Kind,
            newName: Name?,
            annotations: Annotations,
            source: SourceElement
        ): SamAdapterExtensionFunctionDescriptorImpl {
            return SamAdapterExtensionFunctionDescriptorImpl(
                containingDeclaration, original as SimpleFunctionDescriptor?, annotations, newName ?: name, kind, source
            ).apply {
                baseDescriptorForSynthetic = this@SamAdapterExtensionFunctionDescriptorImpl.baseDescriptorForSynthetic
            }
        }

        override fun newCopyBuilder(substitutor: TypeSubstitutor): CopyConfiguration =
            super.newCopyBuilder(substitutor).setOriginal(this.original)

        override fun doSubstitute(configuration: CopyConfiguration): FunctionDescriptor? {
            val descriptor = super.doSubstitute(configuration) as SamAdapterExtensionFunctionDescriptorImpl? ?: return null
            val original = configuration.original
                ?: throw UnsupportedOperationException("doSubstitute with no original should not be called for synthetic extension $this")

            original as SamAdapterExtensionFunctionDescriptorImpl
            assert(original.original == original) { "original in doSubstitute should have no other original" }

            val sourceFunctionSubstitutor =
                CompositionTypeSubstitution(configuration.substitution, fromSourceFunctionTypeParameters).buildSubstitutor()

            descriptor.baseDescriptorForSynthetic = original.baseDescriptorForSynthetic.substitute(sourceFunctionSubstitutor) ?: return null
            return descriptor
        }
    }
}
