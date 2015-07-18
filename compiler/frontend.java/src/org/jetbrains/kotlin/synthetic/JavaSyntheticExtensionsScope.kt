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
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.FileScopeProvider
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.DescriptorSubstitutor
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeFirstWord
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeSmart
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

class AdditionalScopesWithJavaSyntheticExtensions(storageManager: StorageManager) : FileScopeProvider.AdditionalScopes {
    private val scope = JavaSyntheticExtensionsScope(storageManager)

    override fun scopes(file: JetFile) = listOf(scope)
}

class JavaSyntheticExtensionsScope(storageManager: StorageManager) : JetScope by JetScope.Empty {
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
                                .asSequence()
                                .flatMap { memberScope.getFunctions(it).asSequence() }
                                .singleOrNull {
                                    isGoodGetMethod(it) && it.hasJavaOriginInHierarchy()
                                } ?: return null

        // don't accept "uRL" for "getURL" etc
        if (SyntheticJavaPropertyDescriptor.propertyNameByGetMethodName(getMethod.name) != name) return null

        val setMethod = memberScope.getFunctions(setMethodName(getMethod.name))
                .singleOrNull { isGoodSetMethod(it, getMethod) }

        val propertyType = getMethod.returnType ?: return null
        return MyPropertyDescriptor(ownerClass, getMethod.original, setMethod?.original, name, propertyType)
    }

    private fun FunctionDescriptor.hasJavaOriginInHierarchy(): Boolean {
        return if (overriddenDescriptors.isEmpty())
            containingDeclaration is JavaClassDescriptor
        else
            overriddenDescriptors.any { it.hasJavaOriginInHierarchy() }
    }

    private fun isGoodGetMethod(descriptor: FunctionDescriptor): Boolean {
        val returnType = descriptor.returnType ?: return false
        if (returnType.isUnit()) return false
        if (descriptor.name.asString().startsWith("is") && !returnType.isBoolean()) return false

        return descriptor.valueParameters.isEmpty()
               && descriptor.typeParameters.isEmpty()
               && descriptor.visibility == Visibilities.PUBLIC //TODO: what about protected and package-local?
    }

    private fun isGoodSetMethod(descriptor: FunctionDescriptor, getMethod: FunctionDescriptor): Boolean {
        val propertyType = getMethod.returnType ?: return false
        val parameter = descriptor.valueParameters.singleOrNull() ?: return false
        if (parameter.type != propertyType) {
            if (!propertyType.isSubtypeOf(parameter.type)) return false
            if (descriptor.findOverridden {
                val baseProperty = SyntheticJavaPropertyDescriptor.findByGetterOrSetter(it, this)
                baseProperty?.getMethod?.name == getMethod.name
            } == null) return false
        }

        return parameter.varargElementType == null
               && descriptor.typeParameters.isEmpty()
               && descriptor.returnType?.let { it.isUnit() } ?: false
               && descriptor.visibility == Visibilities.PUBLIC
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
        val processedTypes: MutableSet<JetType>? = if (receiverTypes.size() > 1) HashSet<JetType>() else null
        for (type in receiverTypes) {
            result = collectSyntheticPropertiesByName(result, type.makeNotNullable(), name, processedTypes)
        }
        return when {
            result == null -> emptyList()
            result.size() > 1 -> result.toSet()
            else -> result
        }
    }

    private fun collectSyntheticPropertiesByName(result: SmartList<PropertyDescriptor>?, type: JetType, name: Name, processedTypes: MutableSet<JetType>?): SmartList<PropertyDescriptor>? {
        if (processedTypes != null && !processedTypes.add(type)) return result

        @suppress("NAME_SHADOWING")
        var result = result

        val typeConstructor = type.getConstructor()
        val classifier = typeConstructor.getDeclarationDescriptor()
        if (classifier is ClassDescriptor) {
            result = result.add(syntheticPropertyInClass(Pair(classifier, name)))
        }

        typeConstructor.getSupertypes().forEach { result = collectSyntheticPropertiesByName(result, it, name, processedTypes) }

        return result
    }

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<JetType>): Collection<PropertyDescriptor> {
        val result = ArrayList<PropertyDescriptor>()
        val processedTypes = HashSet<JetType>()
        receiverTypes.forEach {
            result.collectSyntheticProperties(it.makeNotNullable(), processedTypes)
        }
        return result
    }

    private fun MutableList<PropertyDescriptor>.collectSyntheticProperties(type: JetType, processedTypes: MutableSet<JetType>) {
        if (!processedTypes.add(type)) return

        val typeConstructor = type.getConstructor()
        val classifier = typeConstructor.getDeclarationDescriptor()
        if (classifier is ClassDescriptor) {
            for (descriptor in classifier.getUnsubstitutedMemberScope().getDescriptors(DescriptorKindFilter.FUNCTIONS)) {
                if (descriptor is FunctionDescriptor) {
                    val propertyName = SyntheticJavaPropertyDescriptor.propertyNameByGetMethodName(descriptor.getName()) ?: continue
                    addIfNotNull(syntheticPropertyInClass(Pair(classifier, propertyName)))
                }
            }
        }

        typeConstructor.getSupertypes().forEach { collectSyntheticProperties(it, processedTypes) }
    }

    private fun SmartList<PropertyDescriptor>?.add(property: PropertyDescriptor?): SmartList<PropertyDescriptor>? {
        if (property == null) return this
        val list = if (this != null) this else SmartList()
        list.add(property)
        return list
    }

    //TODO: reuse code with generation?

    private fun possibleGetMethodNames(propertyName: Name): Collection<Name> {
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
        return result
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

    private class MyPropertyDescriptor(
            ownerClass: ClassDescriptor,
            override val getMethod: FunctionDescriptor,
            override val setMethod: FunctionDescriptor?,
            name: Name,
            type: JetType
    ) : SyntheticJavaPropertyDescriptor, PropertyDescriptorImpl(
            DescriptorUtils.getContainingModule(ownerClass)/* TODO:is it ok? */,
            null,
            Annotations.EMPTY,
            Modality.FINAL,
            Visibilities.PUBLIC,
            setMethod != null,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE/*TODO?*/
    ) {
        init {
            val classTypeParams = ownerClass.typeConstructor.parameters
            val typeParameters = ArrayList<TypeParameterDescriptor>(classTypeParams.size())
            val typeSubstitutor = DescriptorSubstitutor.substituteTypeParameters(classTypeParams, TypeSubstitutor.EMPTY, this, typeParameters)

            val propertyType = typeSubstitutor.safeSubstitute(type, Variance.INVARIANT)
            val receiverType = typeSubstitutor.safeSubstitute(ownerClass.defaultType, Variance.INVARIANT)
            setType(propertyType, typeParameters, null, receiverType)

            val getter = PropertyGetterDescriptorImpl(this,
                                                      Annotations.EMPTY,
                                                      Modality.FINAL,
                                                      Visibilities.PUBLIC,
                                                      false,
                                                      false,
                                                      CallableMemberDescriptor.Kind.SYNTHESIZED,
                                                      null,
                                                      SourceElement.NO_SOURCE/*TODO*/)
            getter.initialize(null)

            val setter = if (setMethod != null)
                PropertySetterDescriptorImpl(this,
                                             Annotations.EMPTY,
                                             Modality.FINAL,
                                             Visibilities.PUBLIC,
                                             false,
                                             false,
                                             CallableMemberDescriptor.Kind.SYNTHESIZED,
                                             null,
                                             SourceElement.NO_SOURCE/*TODO*/)
            else
                null
            setter?.initializeDefault()

            initialize(getter, setter)
        }
    }
}