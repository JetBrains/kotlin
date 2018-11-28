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

        fun propertyNamesByAccessorName(name: Name): List<Name> = listOfNotNull(
            propertyNameByGetMethodName(name),
            propertyNameBySetMethodName(name, withIsPrefix = true),
            propertyNameBySetMethodName(name, withIsPrefix = false)
        )

        fun findByGetterOrSetter(getterOrSetter: FunctionDescriptor, syntheticScope: SyntheticScope) =
            findByGetterOrSetter(getterOrSetter,
                                 object : SyntheticScopes {
                                     override val scopes: Collection<SyntheticScope> = listOf(syntheticScope)
                                 })

        fun propertyNameByGetMethodName(methodName: Name): Name? = org.jetbrains.kotlin.load.java.propertyNameByGetMethodName(methodName)

        fun propertyNameBySetMethodName(methodName: Name, withIsPrefix: Boolean): Name? =
            org.jetbrains.kotlin.load.java.propertyNameBySetMethodName(methodName, withIsPrefix)
    }
}

class JavaSyntheticPropertiesScope(storageManager: StorageManager, private val lookupTracker: LookupTracker) : SyntheticScope {
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

        fun result(descriptor: PropertyDescriptor?, getterNames: List<Name>, setterName: Name? = null): SyntheticPropertyHolder {
            if (lookupTracker === LookupTracker.DO_NOTHING) {
                return if (descriptor == null) SyntheticPropertyHolder.EMPTY else SyntheticPropertyHolder(descriptor, emptyList())
            }

            val names = ArrayList<Name>(getterNames.size + (setterName?.let { 1 } ?: 0))

            names.addAll(getterNames)
            names.addIfNotNull(setterName)

            return SyntheticPropertyHolder(descriptor, names)
        }

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
            } ?: return result(null, possibleGetMethodNames)


        val setMethodName = setMethodName(getMethod.name)
        val setMethod = memberScope.getContributedFunctions(setMethodName, NoLookupLocation.FROM_SYNTHETIC_SCOPE)
            .singleOrNull { isGoodSetMethod(it, getMethod) }

        val propertyType = getMethod.returnType!!

        val descriptor = MyPropertyDescriptor.create(ownerClass, getMethod.original, setMethod?.original, name, propertyType)
        return result(descriptor, possibleGetMethodNames, setMethodName)
    }

    private fun isGoodGetMethod(descriptor: FunctionDescriptor): Boolean {
        val returnType = descriptor.returnType ?: return false
        if (returnType.isUnit()) return false

        return descriptor.valueParameters.isEmpty()
                && descriptor.typeParameters.isEmpty()
                && descriptor.visibility.isVisibleOutside()
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

    override fun getSyntheticStaticFunctions(scope: ResolutionScope, name: Name, location: LookupLocation): Collection<FunctionDescriptor> =
        emptyList()

    override fun getSyntheticConstructors(scope: ResolutionScope, name: Name, location: LookupLocation): Collection<FunctionDescriptor> =
        emptyList()

    override fun getSyntheticStaticFunctions(scope: ResolutionScope): Collection<FunctionDescriptor> = emptyList()

    override fun getSyntheticConstructors(scope: ResolutionScope): Collection<FunctionDescriptor> = emptyList()

    override fun getSyntheticConstructor(constructor: ConstructorDescriptor): ConstructorDescriptor? = null

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

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>): Collection<PropertyDescriptor> {
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
                    val propertyName = SyntheticJavaPropertyDescriptor.propertyNameByGetMethodName(descriptor.getName()) ?: continue
                    addIfNotNull(syntheticPropertyInClass(Pair(classifier, propertyName)).descriptor)
                }
            }
        } else {
            type.supertypes.forEach { collectSyntheticProperties(it.constructor, processedTypes) }
        }
    }

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
        visibility: Visibility,
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
            newVisibility: Visibility,
            original: PropertyDescriptor?,
            kind: CallableMemberDescriptor.Kind,
            newName: Name
        ): PropertyDescriptorImpl {
            return MyPropertyDescriptor(newOwner, this, annotations, newModality, newVisibility, isVar, newName, kind, source).apply {
                getMethod = this@MyPropertyDescriptor.getMethod
                setMethod = this@MyPropertyDescriptor.setMethod
            }
        }

        override fun substitute(originalSubstitutor: TypeSubstitutor): PropertyDescriptor? {
            val descriptor = super.substitute(originalSubstitutor) as MyPropertyDescriptor? ?: return null
            if (descriptor == this) return descriptor

            val classTypeParameters = (getMethod.containingDeclaration as ClassDescriptor).typeConstructor.parameters
            val substitutionMap = HashMap<TypeConstructor, TypeProjection>()
            for ((typeParameter, classTypeParameter) in typeParameters.zip(classTypeParameters)) {
                val typeProjection = originalSubstitutor.substitution[typeParameter.defaultType] ?: continue
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

    companion object {
        fun possibleGetMethodNames(propertyName: Name): List<Name> {
            val result = ArrayList<Name>(3)
            val identifier = propertyName.identifier

            if (JvmAbi.startsWithIsPrefix(identifier)) {
                result.add(propertyName)
            }

            val capitalize1 = identifier.capitalizeAsciiOnly()
            val capitalize2 = identifier.capitalizeFirstWord(asciiOnly = true)
            result.add(Name.identifier("get" + capitalize1))
            if (capitalize2 != capitalize1) {
                result.add(Name.identifier("get" + capitalize2))
            }

            return result
                .filter { SyntheticJavaPropertyDescriptor.propertyNameByGetMethodName(it) == propertyName } // don't accept "uRL" for "getURL" etc
        }

        fun setMethodName(getMethodName: Name): Name {
            val identifier = getMethodName.identifier
            val prefix = when {
                identifier.startsWith("get") -> "get"
                identifier.startsWith("is") -> "is"
                else -> throw IllegalArgumentException()
            }
            return Name.identifier("set" + identifier.removePrefix(prefix))
        }
    }
}
