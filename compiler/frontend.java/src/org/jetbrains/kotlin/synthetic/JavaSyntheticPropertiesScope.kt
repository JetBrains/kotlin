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

package org.jetbrains.kotlin.synthetic

import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.JetScopeImpl
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeFirstWord
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeSmart
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.ArrayList
import java.util.HashSet

interface SyntheticJavaPropertyDescriptor : PropertyDescriptor {
    val getMethod: FunctionDescriptor
    val setMethod: FunctionDescriptor?

    companion object {
        fun findByGetterOrSetter(getterOrSetter: FunctionDescriptor, resolutionScope: JetScope): SyntheticJavaPropertyDescriptor? {
            val name = getterOrSetter.getName()
            if (name.isSpecial()) return null
            val identifier = name.getIdentifier()
            if (!identifier.startsWith("get") && !identifier.startsWith("is") && !identifier.startsWith("set")) return null // optimization

            val owner = getterOrSetter.getContainingDeclaration() as? ClassDescriptor ?: return null

            val originalGetterOrSetter = getterOrSetter.original
            return resolutionScope.getSyntheticExtensionProperties(listOf(owner.getDefaultType()))
                    .filterIsInstance<SyntheticJavaPropertyDescriptor>()
                    .firstOrNull { originalGetterOrSetter == it.getMethod || originalGetterOrSetter == it.setMethod }
        }

        fun propertyNameByGetMethodName(methodName: Name): Name?
                = propertyNameFromAccessorMethodName(methodName, "get") ?: propertyNameFromAccessorMethodName(methodName, "is", removePrefix = false)

        fun propertyNameBySetMethodName(methodName: Name, withIsPrefix: Boolean): Name?
                = propertyNameFromAccessorMethodName(methodName, "set", addPrefix = if (withIsPrefix) "is" else null)

        private fun propertyNameFromAccessorMethodName(methodName: Name, prefix: String, removePrefix: Boolean = true, addPrefix: String? = null): Name? {
            if (methodName.isSpecial()) return null
            val identifier = methodName.getIdentifier()
            if (!identifier.startsWith(prefix)) return null

            if (addPrefix != null) {
                assert(removePrefix)
                return Name.identifier(addPrefix + identifier.removePrefix(prefix))
            }

            if (!removePrefix) return methodName
            val name = identifier.removePrefix(prefix).decapitalizeSmart()
            if (!Name.isValidIdentifier(name)) return null
            return Name.identifier(name)
        }
    }
}

class JavaSyntheticPropertiesScope(storageManager: StorageManager) : JetScopeImpl() {
    private val syntheticPropertyInClass = storageManager.createMemoizedFunctionWithNullableValues<Pair<ClassDescriptor, Name>, PropertyDescriptor> { pair ->
        syntheticPropertyInClassNotCached(pair.first, pair.second)
    }

    private fun syntheticPropertyInClassNotCached(ownerClass: ClassDescriptor, name: Name): PropertyDescriptor? {
        if (name.isSpecial()) return null
        val identifier = name.identifier
        if (identifier.isEmpty()) return null
        val firstChar = identifier[0]
        if (!firstChar.isJavaIdentifierStart() || firstChar.isUpperCase()) return null

        val memberScope = ownerClass.unsubstitutedMemberScope
        val getMethod = possibleGetMethodNames(name)
                                .flatMap { memberScope.getFunctions(it).asSequence() }
                                .singleOrNull {
                                    isGoodGetMethod(it) && it.hasJavaOriginInHierarchy()
                                } ?: return null


        val setMethod = memberScope.getFunctions(setMethodName(getMethod.name))
                .singleOrNull { isGoodSetMethod(it, getMethod) }

        val propertyType = getMethod.returnType ?: return null
        return MyPropertyDescriptor(ownerClass, getMethod.original, setMethod?.original, name, propertyType)
    }

    private fun isGoodGetMethod(descriptor: FunctionDescriptor): Boolean {
        val returnType = descriptor.returnType ?: return false
        if (returnType.isUnit()) return false
        if (descriptor.name.asString().startsWith("is") && !returnType.isBoolean()) return false

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
               && descriptor.returnType?.let { it.isUnit() } ?: false
               && descriptor.visibility.isVisibleOutside()
    }

    private fun FunctionDescriptor.findOverridden(condition: (FunctionDescriptor) -> Boolean): FunctionDescriptor? {
        for (descriptor in overriddenDescriptors) {
            if (condition(descriptor)) return descriptor
            descriptor.findOverridden(condition)?.let { return it }
        }
        return null
    }

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<JetType>, name: Name): Collection<PropertyDescriptor> {
        var result: SmartList<PropertyDescriptor>? = null
        val processedTypes: MutableSet<TypeConstructor>? = if (receiverTypes.size() > 1) HashSet<TypeConstructor>() else null
        for (type in receiverTypes) {
            result = collectSyntheticPropertiesByName(result, type.constructor, name, processedTypes)
        }
        return when {
            result == null -> emptyList()
            result.size() > 1 -> result.toSet()
            else -> result
        }
    }

    private fun collectSyntheticPropertiesByName(result: SmartList<PropertyDescriptor>?, type: TypeConstructor, name: Name, processedTypes: MutableSet<TypeConstructor>?): SmartList<PropertyDescriptor>? {
        if (processedTypes != null && !processedTypes.add(type)) return result

        @suppress("NAME_SHADOWING")
        var result = result

        val classifier = type.declarationDescriptor
        if (classifier is ClassDescriptor) {
            result = result.add(syntheticPropertyInClass(Pair(classifier, name)))
        }
        else {
            type.supertypes.forEach { result = collectSyntheticPropertiesByName(result, it.constructor, name, processedTypes) }
        }

        return result
    }

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<JetType>): Collection<PropertyDescriptor> {
        val result = ArrayList<PropertyDescriptor>()
        val processedTypes = HashSet<TypeConstructor>()
        receiverTypes.forEach { result.collectSyntheticProperties(it.constructor, processedTypes) }
        return result
    }

    private fun MutableList<PropertyDescriptor>.collectSyntheticProperties(type: TypeConstructor, processedTypes: MutableSet<TypeConstructor>) {
        if (!processedTypes.add(type)) return

        val classifier = type.declarationDescriptor
        if (classifier is ClassDescriptor) {
            for (descriptor in classifier.getUnsubstitutedMemberScope().getDescriptors(DescriptorKindFilter.FUNCTIONS)) {
                if (descriptor is FunctionDescriptor) {
                    val propertyName = SyntheticJavaPropertyDescriptor.propertyNameByGetMethodName(descriptor.getName()) ?: continue
                    addIfNotNull(syntheticPropertyInClass(Pair(classifier, propertyName)))
                }
            }
        }
        else {
            type.supertypes.forEach { collectSyntheticProperties(it.constructor, processedTypes) }
        }
    }

    private fun SmartList<PropertyDescriptor>?.add(property: PropertyDescriptor?): SmartList<PropertyDescriptor>? {
        if (property == null) return this
        val list = if (this != null) this else SmartList()
        list.add(property)
        return list
    }

    //TODO: reuse code with generation?

    private fun possibleGetMethodNames(propertyName: Name): Sequence<Name> {
        val result = ArrayList<Name>(3)
        val identifier = propertyName.identifier

        if (identifier.startsWith("is")) {
            result.add(propertyName)
        }

        val capitalize1 = identifier.capitalize()
        val capitalize2 = identifier.capitalizeFirstWord()
        result.add(Name.identifier("get" + capitalize1))
        if (capitalize2 != capitalize1) {
            result.add(Name.identifier("get" + capitalize2))
        }

        return result.asSequence()
                .filter { SyntheticJavaPropertyDescriptor.propertyNameByGetMethodName(it) == propertyName } // don't accept "uRL" for "getURL" etc
    }

    private fun setMethodName(getMethodName: Name): Name {
        val identifier = getMethodName.identifier
        val prefix = when {
            identifier.startsWith("get") -> "get"
            identifier.startsWith("is") -> "is"
            else -> throw IllegalArgumentException()
        }
        return Name.identifier("set" + identifier.removePrefix(prefix))
    }

    override fun getContainingDeclaration(): DeclarationDescriptor {
        throw UnsupportedOperationException()
    }

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.simpleName)
    }

    private class MyPropertyDescriptor(
            ownerClass: ClassDescriptor,
            override val getMethod: FunctionDescriptor,
            override val setMethod: FunctionDescriptor?,
            name: Name,
            type: JetType
    ) : SyntheticJavaPropertyDescriptor, PropertyDescriptorImpl(
            DescriptorUtils.getContainingModule(ownerClass),
            null,
            Annotations.EMPTY,
            Modality.FINAL,
            syntheticExtensionVisibility(getMethod),
            setMethod != null,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
    ) {
        init {
            val classTypeParams = ownerClass.typeConstructor.parameters
            val typeParameters = ArrayList<TypeParameterDescriptor>(classTypeParams.size())
            val typeSubstitutor = DescriptorSubstitutor.substituteTypeParameters(classTypeParams, TypeSubstitution.EMPTY, this, typeParameters)

            val propertyType = typeSubstitutor.safeSubstitute(type, Variance.INVARIANT)
            val receiverType = typeSubstitutor.safeSubstitute(ownerClass.defaultType, Variance.INVARIANT)
            setType(propertyType, typeParameters, null, receiverType)

            val getter = PropertyGetterDescriptorImpl(this,
                                                      Annotations.EMPTY,
                                                      Modality.FINAL,
                                                      visibility,
                                                      false,
                                                      false,
                                                      CallableMemberDescriptor.Kind.SYNTHESIZED,
                                                      null,
                                                      SourceElement.NO_SOURCE)
            getter.initialize(null)

            val setter = if (setMethod != null)
                PropertySetterDescriptorImpl(this,
                                             Annotations.EMPTY,
                                             Modality.FINAL,
                                             syntheticExtensionVisibility(setMethod),
                                             false,
                                             false,
                                             CallableMemberDescriptor.Kind.SYNTHESIZED,
                                             null,
                                             SourceElement.NO_SOURCE)
            else
                null
            setter?.initializeDefault()

            initialize(getter, setter)
        }
    }
}