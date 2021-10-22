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
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.possibleGetMethodNames
import org.jetbrains.kotlin.load.java.setMethodName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeFirstWord
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*
import kotlin.properties.Delegates

fun canBePropertyAccessor(identifier: String): Boolean {
    return identifier.startsWith("get") || identifier.startsWith("is") || identifier.startsWith("set")
}

interface SyntheticJavaPropertyDescriptor : PropertyDescriptor, SyntheticPropertyDescriptor {
    override val getMethod: FunctionDescriptor
    override val setMethod: FunctionDescriptor?

    companion object {
        fun findByGetterOrSetter(getterOrSetter: FunctionDescriptor, syntheticScopes: SyntheticScopes): SyntheticJavaPropertyDescriptor? {
            val name = getterOrSetter.name
            if (name.isSpecial) return null
            val identifier = name.identifier
            if (!canBePropertyAccessor(identifier)) return null  // optimization

            val classDescriptorOwner = getterOrSetter.containingDeclaration as? ClassDescriptor ?: return null

            val originalGetterOrSetter = getterOrSetter.original

            val names = propertyNamesByAccessorName(name)

            return names
                .flatMap {
                    syntheticScopes.collectSyntheticExtensionProperties(
                        listOf(classDescriptorOwner.defaultType),
                        it,
                        NoLookupLocation.FROM_SYNTHETIC_SCOPE
                    )
                }.filterIsInstance<SyntheticJavaPropertyDescriptor>()
                .firstOrNull { originalGetterOrSetter == it.getMethod || originalGetterOrSetter == it.setMethod }
        }

        fun propertyNamesByAccessorName(name: Name): List<Name> =
            org.jetbrains.kotlin.load.java.propertyNamesByAccessorName(name)

        fun findByGetterOrSetter(getterOrSetter: FunctionDescriptor, syntheticScope: SyntheticScope) =
            findByGetterOrSetter(getterOrSetter,
                                 object : SyntheticScopes {
                                     override val scopes: Collection<SyntheticScope> = listOf(syntheticScope)
                                 })

        fun propertyNameByGetMethodName(methodName: Name): Name? = org.jetbrains.kotlin.load.java.propertyNameByGetMethodName(methodName)
    }
}

class JavaSyntheticPropertiesScope(
    storageManager: StorageManager,
    private val lookupTracker: LookupTracker,
    private val supportJavaRecords: Boolean,
) : SyntheticScope.Default() {
    private val syntheticPropertyInClass =
        storageManager.createMemoizedFunction<Pair<ClassDescriptor, Name>, SyntheticPropertyHolder> { pair ->
            syntheticPropertyInClassNotCached(pair.first, pair.second)
        }

    private fun getSyntheticPropertyAndRecordLookups(
        classifier: ClassDescriptor,
        name: Name,
        location: LookupLocation
    ): PropertyDescriptor? {
        val (descriptor, lookedNames) = syntheticPropertyInClass(Pair(classifier, name))

        if (location !is NoLookupLocation) {
            lookedNames.forEach { lookupTracker.record(location, classifier, it) }
        }

        return descriptor
    }

    private fun syntheticPropertyInClassNotCached(ownerClass: ClassDescriptor, name: Name): SyntheticPropertyHolder {
        val forBean = syntheticPropertyHolderForBeanConvention(name, ownerClass)
        if (forBean.descriptor != null) return forBean

        if (!ownerClass.isRecord()) return forBean

        val propertyForComponent = syntheticPropertyDescriptorForRecordComponent(name, ownerClass)

        return createSyntheticPropertyHolder(propertyForComponent, forBean.lookedNames, name)
    }

    private fun createSyntheticPropertyHolder(
        descriptor: PropertyDescriptor?,
        lookedNames: List<Name>,
        additionalName: Name? = null
    ): SyntheticPropertyHolder {
        if (lookupTracker === LookupTracker.DO_NOTHING) {
            return if (descriptor == null) SyntheticPropertyHolder.EMPTY else SyntheticPropertyHolder(descriptor, emptyList())
        }

        val names = ArrayList<Name>(lookedNames.size + (additionalName?.let { 1 } ?: 0))

        names.addAll(lookedNames)
        names.addIfNotNull(additionalName)

        return SyntheticPropertyHolder(descriptor, names)
    }

    private fun syntheticPropertyHolderForBeanConvention(
        name: Name,
        ownerClass: ClassDescriptor
    ): SyntheticPropertyHolder {
        if (name.isSpecial) return SyntheticPropertyHolder.EMPTY

        val identifier = name.identifier
        if (identifier.isEmpty()) return SyntheticPropertyHolder.EMPTY

        val firstChar = identifier[0]
        if (!firstChar.isJavaIdentifierStart() || firstChar in 'A'..'Z') return SyntheticPropertyHolder.EMPTY

        val memberScope = ownerClass.unsubstitutedMemberScope

        val possibleGetMethodNames = possibleGetMethodNames(name)
        val getMethod = possibleGetMethodNames
            .flatMap { memberScope.getContributedFunctions(it, NoLookupLocation.FROM_SYNTHETIC_SCOPE) }
            .singleOrNull {
                it.hasJavaOriginInHierarchy() && isGoodGetMethod(it)
            } ?: return createSyntheticPropertyHolder(null, possibleGetMethodNames)


        val setMethodName = setMethodName(getMethod.name)
        val setMethod = memberScope.getContributedFunctions(setMethodName, NoLookupLocation.FROM_SYNTHETIC_SCOPE)
            .singleOrNull { isGoodSetMethod(it, getMethod) }

        val propertyType = getMethod.returnType!!

        val descriptor = MyPropertyDescriptor.create(ownerClass, getMethod.original, setMethod?.original, name, propertyType)
        return createSyntheticPropertyHolder(descriptor, possibleGetMethodNames, setMethodName)
    }

    private fun syntheticPropertyDescriptorForRecordComponent(
        name: Name,
        ownerClass: ClassDescriptor
    ): PropertyDescriptor? {
        if (!supportJavaRecords) return null

        val componentLikeMethod =
            ownerClass.unsubstitutedMemberScope
                .getContributedFunctions(name, NoLookupLocation.FROM_SYNTHETIC_SCOPE)
                .singleOrNull(this::isGoodGetMethod) ?: return null

        if (componentLikeMethod !is JavaMethodDescriptor || !componentLikeMethod.isForRecordComponent) {
            return null
        }

        val propertyType = componentLikeMethod.returnType!!

        return MyPropertyDescriptor.create(ownerClass, componentLikeMethod.original, null, name, propertyType)
    }

    private fun isGoodGetMethod(descriptor: FunctionDescriptor): Boolean {
        val returnType = descriptor.returnType ?: return false
        if (returnType.isUnit()) return false

        return descriptor.valueParameters.isEmpty()
                && descriptor.typeParameters.isEmpty()
                && descriptor.visibility.isVisibleOutside()
                && !(descriptor.isHiddenForResolutionEverywhereBesideSupercalls && descriptor.name.asString() == "isEmpty") // CharSequence.isEmpty() from JDK15
    }

    private fun isGoodSetMethod(descriptor: FunctionDescriptor, getMethod: FunctionDescriptor): Boolean {
        val propertyType = getMethod.returnType ?: return false
        val parameter = descriptor.valueParameters.singleOrNull() ?: return false
        if (!TypeUtils.equalTypes(parameter.type, propertyType)) {
            if (!propertyType.isSubtypeOf(parameter.type)) return false
            if (descriptor.findOverridden {
                    val baseProperty = SyntheticJavaPropertyDescriptor.findByGetterOrSetter(it, this)
                    baseProperty?.getMethod?.name == getMethod.name
                } == null) return false
        }

        return parameter.varargElementType == null
                && descriptor.typeParameters.isEmpty()
                && descriptor.visibility.isVisibleOutside()
                && !(descriptor.isHiddenForResolutionEverywhereBesideSupercalls && descriptor.name.asString() == "isEmpty") // CharSequence.isEmpty() from JDK15
    }

    private fun FunctionDescriptor.findOverridden(condition: (FunctionDescriptor) -> Boolean): FunctionDescriptor? {
        for (descriptor in overriddenDescriptors) {
            if (condition(descriptor)) return descriptor
            descriptor.findOverridden(condition)?.let { return it }
        }
        return null
    }

    override fun getSyntheticExtensionProperties(
        receiverTypes: Collection<KotlinType>,
        name: Name,
        location: LookupLocation
    ): Collection<PropertyDescriptor> {
        var result: SmartList<PropertyDescriptor>? = null
        val processedTypes: MutableSet<TypeConstructor>? = if (receiverTypes.size > 1) HashSet<TypeConstructor>() else null
        for (type in receiverTypes) {
            result = collectSyntheticPropertiesByName(result, type.constructor, name, processedTypes, location)
        }
        return when {
            result == null -> emptyList()
            result.size > 1 -> result.toSet()
            else -> result
        }
    }

    private fun collectSyntheticPropertiesByName(
        result: SmartList<PropertyDescriptor>?,
        type: TypeConstructor,
        name: Name,
        processedTypes: MutableSet<TypeConstructor>?,
        location: LookupLocation
    ): SmartList<PropertyDescriptor>? {
        if (processedTypes != null && !processedTypes.add(type)) return result

        @Suppress("NAME_SHADOWING")
        var result = result

        val classifier = type.declarationDescriptor
        if (classifier is ClassDescriptor) {
            result = result.add(getSyntheticPropertyAndRecordLookups(classifier, name, location))
        } else {
            type.supertypes.forEach { result = collectSyntheticPropertiesByName(result, it.constructor, name, processedTypes, location) }
        }

        return result
    }

    override fun getSyntheticExtensionProperties(
        receiverTypes: Collection<KotlinType>,
        location: LookupLocation
    ): Collection<PropertyDescriptor> {
        val result = ArrayList<PropertyDescriptor>()
        val processedTypes = HashSet<TypeConstructor>()
        receiverTypes.forEach { result.collectSyntheticProperties(it.constructor, processedTypes) }
        return result
    }

    private fun MutableList<PropertyDescriptor>.collectSyntheticProperties(
        type: TypeConstructor,
        processedTypes: MutableSet<TypeConstructor>
    ) {
        if (!processedTypes.add(type)) return

        val classifier = type.declarationDescriptor
        if (classifier is ClassDescriptor) {
            for (descriptor in classifier.unsubstitutedMemberScope.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)) {
                if (descriptor is FunctionDescriptor) {
                    val propertyName = SyntheticJavaPropertyDescriptor.propertyNameByGetMethodName(descriptor.getName())
                    if (propertyName != null) {
                        addIfNotNull(syntheticPropertyInClass(Pair(classifier, propertyName)).descriptor)
                    }

                    if (classifier.isRecord()) {
                        addIfNotNull(syntheticPropertyInClass(Pair(classifier, descriptor.name)).descriptor)
                    }
                }
            }
        } else {
            type.supertypes.forEach { collectSyntheticProperties(it.constructor, processedTypes) }
        }
    }

    private fun ClassifierDescriptor.isRecord() =
        this is JavaClassDescriptor && isRecord

    private fun SmartList<PropertyDescriptor>?.add(property: PropertyDescriptor?): SmartList<PropertyDescriptor>? {
        if (property == null) return this
        val list = if (this != null) this else SmartList()
        list.add(property)
        return list
    }

    override fun getSyntheticMemberFunctions(
        receiverTypes: Collection<KotlinType>,
        name: Name,
        location: LookupLocation
    ): Collection<FunctionDescriptor> = emptyList()

    override fun getSyntheticMemberFunctions(receiverTypes: Collection<KotlinType>): Collection<FunctionDescriptor> = emptyList()

    private data class SyntheticPropertyHolder(val descriptor: PropertyDescriptor?, val lookedNames: List<Name>) {
        companion object {
            val EMPTY = SyntheticPropertyHolder(null, emptyList())
        }
    }

    private class MyPropertyDescriptor(
        containingDeclaration: DeclarationDescriptor,
        original: PropertyDescriptor?,
        annotations: Annotations,
        modality: Modality,
        visibility: DescriptorVisibility,
        isVar: Boolean,
        name: Name,
        kind: CallableMemberDescriptor.Kind,
        source: SourceElement
    ) : SyntheticJavaPropertyDescriptor, PropertyDescriptorImpl(
        containingDeclaration, original, annotations, modality, visibility, isVar, name, kind, source,
        /* lateInit = */ false, /* isConst = */ false, /* isExpect = */ false, /* isActual = */ false, /* isExternal = */ false,
        /* isDelegated = */ false
    ) {

        override var getMethod: FunctionDescriptor by Delegates.notNull()
            private set

        override var setMethod: FunctionDescriptor? = null
            private set

        companion object {
            fun create(
                ownerClass: ClassDescriptor,
                getMethod: FunctionDescriptor,
                setMethod: FunctionDescriptor?,
                name: Name,
                type: KotlinType
            ): MyPropertyDescriptor {
                val visibility = syntheticVisibility(getMethod, isUsedForExtension = true)
                val descriptor = MyPropertyDescriptor(
                    DescriptorUtils.getContainingModule(ownerClass),
                    null,
                    Annotations.EMPTY,
                    Modality.FINAL,
                    visibility,
                    setMethod != null,
                    name,
                    CallableMemberDescriptor.Kind.SYNTHESIZED,
                    SourceElement.NO_SOURCE
                )
                descriptor.getMethod = getMethod
                descriptor.setMethod = setMethod

                val classTypeParams = ownerClass.typeConstructor.parameters
                val typeParameters = ArrayList<TypeParameterDescriptor>(classTypeParams.size)
                val typeSubstitutor =
                    DescriptorSubstitutor.substituteTypeParameters(classTypeParams, TypeSubstitution.EMPTY, descriptor, typeParameters)

                val propertyType = typeSubstitutor.safeSubstitute(type, Variance.INVARIANT)
                val receiverType = typeSubstitutor.safeSubstitute(ownerClass.defaultType, Variance.INVARIANT)
                descriptor.setType(
                    propertyType, typeParameters, null,
                    DescriptorFactory.createExtensionReceiverParameterForCallable(descriptor, receiverType, Annotations.EMPTY)
                )

                val getter = PropertyGetterDescriptorImpl(
                    descriptor,
                    getMethod.annotations,
                    Modality.FINAL,
                    visibility,
                    false,
                    getMethod.isExternal,
                    false,
                    CallableMemberDescriptor.Kind.SYNTHESIZED,
                    null,
                    SourceElement.NO_SOURCE
                )
                getter.initialize(null)

                val setter = if (setMethod != null)
                    PropertySetterDescriptorImpl(
                        descriptor,
                        setMethod.annotations,
                        Modality.FINAL,
                        syntheticVisibility(setMethod, isUsedForExtension = true),
                        false,
                        setMethod.isExternal,
                        false,
                        CallableMemberDescriptor.Kind.SYNTHESIZED,
                        null,
                        SourceElement.NO_SOURCE
                    )
                else
                    null
                setter?.initializeDefault()

                descriptor.initialize(getter, setter)

                return descriptor
            }
        }

        override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            newModality: Modality,
            newVisibility: DescriptorVisibility,
            original: PropertyDescriptor?,
            kind: CallableMemberDescriptor.Kind,
            newName: Name,
            source: SourceElement
        ): PropertyDescriptorImpl {
            return MyPropertyDescriptor(newOwner, this, annotations, newModality, newVisibility, isVar, newName, kind, this.source).apply {
                getMethod = this@MyPropertyDescriptor.getMethod
                setMethod = this@MyPropertyDescriptor.setMethod
            }
        }

        override fun substitute(substitutor: TypeSubstitutor): PropertyDescriptor? {
            val descriptor = super.substitute(substitutor) as MyPropertyDescriptor? ?: return null
            if (descriptor == this) return descriptor

            val classTypeParameters = (getMethod.containingDeclaration as ClassDescriptor).typeConstructor.parameters
            val substitutionMap = HashMap<TypeConstructor, TypeProjection>()
            for ((typeParameter, classTypeParameter) in typeParameters.zip(classTypeParameters)) {
                val typeProjection = substitutor.substitution[typeParameter.defaultType] ?: continue
                substitutionMap[classTypeParameter.typeConstructor] = typeProjection

            }
            val classParametersSubstitutor = TypeConstructorSubstitution.createByConstructorsMap(
                substitutionMap,
                approximateCapturedTypes = true
            ).buildSubstitutor()

            descriptor.getMethod = getMethod.substitute(classParametersSubstitutor) ?: return null
            descriptor.setMethod = setMethod?.substitute(classParametersSubstitutor)
            return descriptor
        }
    }
}
