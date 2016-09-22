/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.lazy.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DELEGATION
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.FAKE_OVERRIDE
import org.jetbrains.kotlin.descriptors.impl.ConstructorDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.NullableLazyValue
import org.jetbrains.kotlin.types.DeferredType
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

open class LazyClassMemberScope(
        c: LazyClassContext,
        declarationProvider: ClassMemberDeclarationProvider,
        thisClass: LazyClassDescriptor,
        trace: BindingTrace
) : AbstractLazyMemberScope<LazyClassDescriptor, ClassMemberDeclarationProvider>(c, declarationProvider, thisClass, trace) {

    private val descriptorsFromDeclaredElements = storageManager.createLazyValue {
        computeDescriptorsFromDeclaredElements(DescriptorKindFilter.ALL, MemberScope.ALL_NAME_FILTER, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)
    }
    private val extraDescriptors: NotNullLazyValue<Collection<DeclarationDescriptor>> = storageManager.createLazyValue {
        computeExtraDescriptors(NoLookupLocation.FOR_ALREADY_TRACKED)
    }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter,
                                           nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        val result = LinkedHashSet(descriptorsFromDeclaredElements())
        result.addAll(extraDescriptors())
        return result
    }

    protected open fun computeExtraDescriptors(location: LookupLocation): Collection<DeclarationDescriptor> {
        val result = ArrayList<DeclarationDescriptor>()
        for (supertype in thisDescriptor.typeConstructor.supertypes) {
            for (descriptor in supertype.memberScope.getContributedDescriptors()) {
                if (descriptor is FunctionDescriptor) {
                    result.addAll(getContributedFunctions(descriptor.name, location))
                }
                else if (descriptor is PropertyDescriptor) {
                    result.addAll(getContributedVariables(descriptor.name, location))
                }
                // Nothing else is inherited
            }
        }

        addDataClassMethods(result, location)

        result.trimToSize()
        return result
    }

    private interface MemberExtractor<out T : CallableMemberDescriptor> {
        fun extract(extractFrom: KotlinType, name: Name): Collection<T>
    }

    private val primaryConstructor: NullableLazyValue<ConstructorDescriptor>
            = c.storageManager.createNullableLazyValue { resolvePrimaryConstructor() }

    override fun getScopeForMemberDeclarationResolution(declaration: KtDeclaration): LexicalScope {
        if (declaration is KtProperty) {
            return thisDescriptor.scopeForInitializerResolution
        }
        return thisDescriptor.scopeForMemberDeclarationResolution
    }

    private fun <D : CallableMemberDescriptor> generateFakeOverrides(name: Name, fromSupertypes: Collection<D>, result: MutableCollection<D>, exactDescriptorClass: Class<out D>) {
        OverridingUtil.generateOverridesInFunctionGroup(name, fromSupertypes, ArrayList(result), thisDescriptor, object : OverridingStrategy() {
            override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                assert(exactDescriptorClass.isInstance(fakeOverride)) { "Wrong descriptor type in an override: " + fakeOverride + " while expecting " + exactDescriptorClass.simpleName }
                @Suppress("UNCHECKED_CAST")
                result.add(fakeOverride as D)
            }

            override fun overrideConflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
                val declaration = DescriptorToSourceUtils.descriptorToDeclaration(fromCurrent) as? KtDeclaration ?: error("fromCurrent can not be a fake override")
                trace.report(Errors.CONFLICTING_OVERLOADS.on(declaration, fromCurrent, fromSuper.containingDeclaration))
            }

            override fun inheritanceConflict(first: CallableMemberDescriptor, second: CallableMemberDescriptor) {
                val thisClassDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(thisDescriptor) as? KtClassOrObject ?: error("No class declaration")
                trace.report(Errors.CONFLICTING_INHERITED_MEMBERS.on(thisClassDeclaration, thisDescriptor, listOf(first, second)))
            }
        })
        OverrideResolver.resolveUnknownVisibilities(result, trace)
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        // TODO: this should be handled by lazy function descriptors
        val functions = super.getContributedFunctions(name, location)
        resolveUnknownVisibilitiesForMembers(functions)
        return functions
    }

    override fun getNonDeclaredFunctions(name: Name, result: MutableSet<SimpleFunctionDescriptor>) {
        val location = NoLookupLocation.FOR_ALREADY_TRACKED

        val fromSupertypes = arrayListOf<SimpleFunctionDescriptor>()
        for (supertype in thisDescriptor.typeConstructor.supertypes) {
            fromSupertypes.addAll(supertype.memberScope.getContributedFunctions(name, location))
        }
        result.addAll(generateDelegatingDescriptors(name, EXTRACT_FUNCTIONS, result))
        generateDataClassMethods(result, name, location, fromSupertypes)
        generateFakeOverrides(name, fromSupertypes, result, SimpleFunctionDescriptor::class.java)
    }

    private fun generateDataClassMethods(
            result: MutableCollection<SimpleFunctionDescriptor>,
            name: Name,
            location: LookupLocation,
            fromSupertypes: List<SimpleFunctionDescriptor>
    ) {
        if (!thisDescriptor.isData) return

        val constructor = getPrimaryConstructor() ?: return

        val primaryConstructorParameters = declarationProvider.getOwnerInfo().primaryConstructorParameters
        assert(constructor.valueParameters.size == primaryConstructorParameters.size) { "From descriptor: " + constructor.valueParameters.size + " but from PSI: " + primaryConstructorParameters.size }

        if (DataClassDescriptorResolver.isComponentLike(name)) {
            var componentIndex = 0

            for (parameter in constructor.valueParameters) {
                if (parameter.type.isError) continue
                if (!primaryConstructorParameters.get(parameter.index).hasValOrVar()) continue

                val properties = getContributedVariables(parameter.name, location)
                if (properties.isEmpty()) continue

                val property = properties.iterator().next()

                ++componentIndex

                if (name == DataClassDescriptorResolver.createComponentName(componentIndex)) {
                    result.add(DataClassDescriptorResolver.createComponentFunctionDescriptor(
                            componentIndex, property, parameter, thisDescriptor, trace
                    ))
                    break
                }
            }
        }

        if (name == DataClassDescriptorResolver.COPY_METHOD_NAME) {
            for (parameter in constructor.valueParameters) {
                // force properties resolution to fill BindingContext.VALUE_PARAMETER_AS_PROPERTY slice
                getContributedVariables(parameter.name, location)
            }

            result.add(DataClassDescriptorResolver.createCopyFunctionDescriptor(constructor.valueParameters, thisDescriptor, trace))
        }

        fun shouldAddFunctionFromAny(checkParameters: (FunctionDescriptor) -> Boolean): Boolean {
            // Add 'equals', 'hashCode', 'toString' iff there is no such declared member AND there is no such final member in supertypes
            return result.none(checkParameters) &&
                   fromSupertypes.none { checkParameters(it) && it.modality == Modality.FINAL }
        }

        if (name == DataClassDescriptorResolver.EQUALS_METHOD_NAME && shouldAddFunctionFromAny { function ->
            val parameters = function.valueParameters
            parameters.size == 1 && KotlinBuiltIns.isNullableAny(parameters.first().type)
        }) {
            result.add(DataClassDescriptorResolver.createEqualsFunctionDescriptor(thisDescriptor))
        }

        if (name == DataClassDescriptorResolver.HASH_CODE_METHOD_NAME && shouldAddFunctionFromAny { it.valueParameters.isEmpty() }) {
            result.add(DataClassDescriptorResolver.createHashCodeFunctionDescriptor(thisDescriptor))
        }

        if (name == DataClassDescriptorResolver.TO_STRING_METHOD_NAME && shouldAddFunctionFromAny { it.valueParameters.isEmpty() }) {
            result.add(DataClassDescriptorResolver.createToStringFunctionDescriptor(thisDescriptor))
        }
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        // TODO: this should be handled by lazy property descriptors
        val properties = super.getContributedVariables(name, location)
        resolveUnknownVisibilitiesForMembers(properties as Collection<CallableMemberDescriptor>)
        return properties
    }

    private fun resolveUnknownVisibilitiesForMembers(descriptors: Collection<CallableMemberDescriptor>) {
        for (descriptor in descriptors) {
            if (descriptor.kind != FAKE_OVERRIDE && descriptor.kind != DELEGATION) {
                OverridingUtil.resolveUnknownVisibilityForMember(descriptor, OverrideResolver.createCannotInferVisibilityReporter(trace))
            }
            VarianceCheckerCore(trace.bindingContext, DiagnosticSink.DO_NOTHING).recordPrivateToThisIfNeeded(descriptor)
        }
    }

    override fun getNonDeclaredProperties(name: Name, result: MutableSet<PropertyDescriptor>) {
        createPropertiesFromPrimaryConstructorParameters(name, result)

        // Members from supertypes
        val fromSupertypes = ArrayList<PropertyDescriptor>()
        for (supertype in thisDescriptor.typeConstructor.supertypes) {
            fromSupertypes.addAll(supertype.memberScope.getContributedVariables(name, NoLookupLocation.FOR_ALREADY_TRACKED))
        }
        result.addAll(generateDelegatingDescriptors(name, EXTRACT_PROPERTIES, result))
        generateFakeOverrides(name, fromSupertypes, result, PropertyDescriptor::class.java)
    }

    protected open fun createPropertiesFromPrimaryConstructorParameters(name: Name, result: MutableSet<PropertyDescriptor>) {
        val classInfo = declarationProvider.getOwnerInfo()

        // From primary constructor parameters
        val primaryConstructor = getPrimaryConstructor() ?: return

        val valueParameterDescriptors = primaryConstructor.valueParameters
        val primaryConstructorParameters = classInfo.primaryConstructorParameters
        assert(valueParameterDescriptors.size == primaryConstructorParameters.size) {
            "From descriptor: ${valueParameterDescriptors.size} but from PSI: ${primaryConstructorParameters.size}"
        }

        for (valueParameterDescriptor in valueParameterDescriptors) {
            if (name != valueParameterDescriptor.name) continue

            val parameter = primaryConstructorParameters.get(valueParameterDescriptor.index)
            if (parameter.hasValOrVar()) {
                val propertyDescriptor = c.descriptorResolver.resolvePrimaryConstructorParameterToAProperty(
                        // TODO: can't test because we get types from cache for this case
                        thisDescriptor, valueParameterDescriptor, thisDescriptor.scopeForConstructorHeaderResolution, parameter, trace)
                result.add(propertyDescriptor)
            }
        }
    }

    private fun <T : CallableMemberDescriptor> generateDelegatingDescriptors(name: Name, extractor: MemberExtractor<T>, existingDescriptors: Collection<CallableDescriptor>): Collection<T> {
        val classOrObject = declarationProvider.getOwnerInfo().correspondingClassOrObject
            ?: return setOf()

        val lazyTypeResolver = object : DelegationResolver.TypeResolver {
            override fun resolve(reference: KtTypeReference): KotlinType? =
                    c.typeResolver.resolveType(thisDescriptor.scopeForClassHeaderResolution, reference, trace, false)
        }
        val lazyMemberExtractor = object : DelegationResolver.MemberExtractor<T> {
            override fun getMembersByType(type: KotlinType): Collection<T> =
                    extractor.extract(type, name)
        }
        return DelegationResolver.generateDelegatedMembers(classOrObject, thisDescriptor, existingDescriptors, trace, lazyMemberExtractor, lazyTypeResolver)
    }

    private fun addDataClassMethods(result: MutableCollection<DeclarationDescriptor>, location: LookupLocation) {
        if (!thisDescriptor.isData) return

        if (getPrimaryConstructor() == null) return

        // Generate componentN functions until there's no such function for some n
        var n = 1
        while (true) {
            val componentName = DataClassDescriptorResolver.createComponentName(n)
            val functions = getContributedFunctions(componentName, location)
            if (functions.isEmpty()) break

            result.addAll(functions)

            n++
        }
        result.addAll(getContributedFunctions(Name.identifier("copy"), location))
    }

    private val secondaryConstructors: NotNullLazyValue<Collection<ConstructorDescriptor>>
            = c.storageManager.createLazyValue { resolveSecondaryConstructors() }

    fun getConstructors(): Collection<ConstructorDescriptor> {
        val result = secondaryConstructors()
        val primaryConstructor = getPrimaryConstructor()
        return if (primaryConstructor == null) result else result + primaryConstructor
    }

    fun getPrimaryConstructor(): ConstructorDescriptor? = primaryConstructor()

    protected open fun resolvePrimaryConstructor(): ConstructorDescriptor? {
        val ownerInfo = declarationProvider.getOwnerInfo()
        val classOrObject = ownerInfo.correspondingClassOrObject ?: return null

        val hasPrimaryConstructor = classOrObject.hasExplicitPrimaryConstructor()
        if (DescriptorUtils.isInterface(thisDescriptor) && !hasPrimaryConstructor) return null

        if (DescriptorUtils.canHaveDeclaredConstructors(thisDescriptor) || hasPrimaryConstructor) {
            val constructor = c.functionDescriptorResolver.resolvePrimaryConstructorDescriptor(
                    thisDescriptor.scopeForConstructorHeaderResolution, thisDescriptor, classOrObject, trace)
            constructor ?: return null
            setDeferredReturnType(constructor)
            return constructor
        }
        else {
            val constructor = DescriptorResolver.createAndRecordPrimaryConstructorForObject(classOrObject, thisDescriptor, trace)
            setDeferredReturnType(constructor)
            return constructor
        }
    }

    private fun resolveSecondaryConstructors(): Collection<ConstructorDescriptor> {
        val classOrObject = declarationProvider.getOwnerInfo().correspondingClassOrObject ?: return emptyList()

        return classOrObject.getSecondaryConstructors().map { constructor ->
            val descriptor = c.functionDescriptorResolver.resolveSecondaryConstructorDescriptor(
                    thisDescriptor.scopeForConstructorHeaderResolution, thisDescriptor, constructor, trace
            )
            setDeferredReturnType(descriptor)
            descriptor
        }
    }

    protected fun setDeferredReturnType(descriptor: ConstructorDescriptorImpl) {
        descriptor.returnType = DeferredType.create(c.storageManager, trace, { thisDescriptor.getDefaultType() })
    }

    override fun recordLookup(name: Name, from: LookupLocation) {
        c.lookupTracker.record(from, thisDescriptor, name)
    }

    // Do not add details here, they may compromise the laziness during debugging
    override fun toString() = "lazy scope for class ${thisDescriptor.name}"

    companion object {
        private val EXTRACT_FUNCTIONS: MemberExtractor<SimpleFunctionDescriptor> = object : MemberExtractor<SimpleFunctionDescriptor> {
            override fun extract(extractFrom: KotlinType, name: Name): Collection<SimpleFunctionDescriptor> {
                return extractFrom.memberScope.getContributedFunctions(name, NoLookupLocation.FOR_ALREADY_TRACKED)
            }
        }

        private val EXTRACT_PROPERTIES: MemberExtractor<PropertyDescriptor> = object : MemberExtractor<PropertyDescriptor> {
            override fun extract(extractFrom: KotlinType, name: Name): Collection<PropertyDescriptor> {
                return extractFrom.memberScope.getContributedVariables(name, NoLookupLocation.FOR_ALREADY_TRACKED)
            }
        }
    }
}
