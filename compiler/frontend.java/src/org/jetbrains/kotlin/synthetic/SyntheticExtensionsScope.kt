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
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.utils.addIfNotNull
import java.beans.Introspector
import java.util.ArrayList

interface SyntheticExtensionPropertyDescriptor : PropertyDescriptor {
    val getMethod: FunctionDescriptor
    val setMethod: FunctionDescriptor?

    companion object {
        fun findByGetterOrSetter(getterOrSetter: FunctionDescriptor, resolutionScope: JetScope): SyntheticExtensionPropertyDescriptor? {
            val name = getterOrSetter.getName()
            if (propertyNameByGetMethodName(name) == null && propertyNameBySetMethodName(name) == null) return null // optimization

            val owner = getterOrSetter.getContainingDeclaration()
            if (owner !is JavaClassDescriptor) return null

            return resolutionScope.getSyntheticExtensionProperties(owner.getDefaultType())
                    .filterIsInstance<SyntheticExtensionPropertyDescriptor>()
                    .firstOrNull { getterOrSetter == it.getMethod || getterOrSetter == it.setMethod }
        }

        fun propertyNameByGetMethodName(methodName: Name): Name?
                = propertyNameFromAccessorMethodName(methodName, "get") ?: propertyNameFromAccessorMethodName(methodName, "is")

        fun propertyNameBySetMethodName(methodName: Name): Name?
                = propertyNameFromAccessorMethodName(methodName, "set")

        private fun propertyNameFromAccessorMethodName(methodName: Name, prefix: String): Name? {
            if (methodName.isSpecial()) return null
            val identifier = methodName.getIdentifier()
            if (!identifier.startsWith(prefix)) return null
            val name = Introspector.decapitalize(identifier.removePrefix(prefix))
            if (!Name.isValidIdentifier(name)) return null
            return Name.identifier(name)
        }
    }
}

class AdditionalScopesWithSyntheticExtensions(storageManager: StorageManager) : FileScopeProvider.AdditionalScopes() {
    private val scope = SyntheticExtensionsScope(storageManager)

    override fun scopes(file: JetFile) = listOf(scope)
}

class SyntheticExtensionsScope(storageManager: StorageManager) : JetScope by JetScope.Empty {
    private val syntheticPropertyInClass = storageManager.createMemoizedFunctionWithNullableValues<Triple<JavaClassDescriptor, JetType, Name>, PropertyDescriptor> { triple ->
        syntheticPropertyInClassNotCached(triple.first, triple.second, triple.third)
    }

    private fun syntheticPropertyInClassNotCached(javaClass: JavaClassDescriptor, type: JetType, name: Name): PropertyDescriptor? {
        if (name.isSpecial()) return null
        val identifier = name.getIdentifier()
        if (identifier.isEmpty()) return null
        val firstChar = identifier[0]
        if (!firstChar.isJavaIdentifierStart()) return null
        if (identifier.length() > 1) {
            if (firstChar.isUpperCase() != identifier[1].isUpperCase()) return null
        }
        else {
            if (firstChar.isUpperCase()) return null
        }

        val memberScope = javaClass.getMemberScope(type.getArguments())
        val getMethod = possibleGetMethodNames(name)
                                .asSequence()
                                .flatMap { memberScope.getFunctions(it).asSequence() }
                                .singleOrNull { isGoodGetMethod(it) } ?: return null

        val propertyType = getMethod.getReturnType() ?: return null
        val setMethod = memberScope.getFunctions(possibleSetMethodName(name)).singleOrNull { isGoodSetMethod(it, propertyType) }

        return MyPropertyDescriptor(javaClass, getMethod, setMethod, name, propertyType, type)
    }

    private fun isGoodGetMethod(descriptor: FunctionDescriptor): Boolean {
        val returnType = descriptor.getReturnType() ?: return false
        if (returnType.isUnit()) return false
        if (descriptor.getName().asString().startsWith("is") && !returnType.isBoolean()) return false

        return descriptor.getValueParameters().isEmpty()
               && descriptor.getTypeParameters().isEmpty()
               && descriptor.getVisibility() == Visibilities.PUBLIC //TODO: what about protected and package-local?
    }

    private fun isGoodSetMethod(descriptor: FunctionDescriptor, propertyType: JetType): Boolean {
        val parameter = descriptor.getValueParameters().singleOrNull() ?: return false
        return parameter.getType() == propertyType
               && parameter.getVarargElementType() == null
               && descriptor.getTypeParameters().isEmpty()
               && descriptor.getReturnType()?.let { it.isUnit() } ?: false
               && descriptor.getVisibility() == Visibilities.PUBLIC
    }

    override fun getSyntheticExtensionProperties(receiverType: JetType, name: Name): Collection<VariableDescriptor> {
        return collectSyntheticPropertiesByName(null, receiverType.makeNotNullable(), name) ?: emptyList()
    }

    private fun collectSyntheticPropertiesByName(result: SmartList<PropertyDescriptor>?, type: JetType, name: Name): SmartList<PropertyDescriptor>? {
        @suppress("NAME_SHADOWING")
        var result = result

        val typeConstructor = type.getConstructor()
        val classifier = typeConstructor.getDeclarationDescriptor()
        if (classifier is JavaClassDescriptor) {
            result = result.add(syntheticPropertyInClass(Triple(classifier, type, name)))
        }

        typeConstructor.getSupertypes().forEach { result = collectSyntheticPropertiesByName(result, it, name) }

        return result
    }

    override fun getSyntheticExtensionProperties(receiverType: JetType): Collection<VariableDescriptor> {
        val result = ArrayList<PropertyDescriptor>()
        result.collectSyntheticProperties(receiverType.makeNotNullable())
        return result
    }

    private fun MutableList<PropertyDescriptor>.collectSyntheticProperties(type: JetType) {
        val typeConstructor = type.getConstructor()
        val classifier = typeConstructor.getDeclarationDescriptor()
        if (classifier is JavaClassDescriptor) {
            for (descriptor in classifier.getMemberScope(type.getArguments()).getAllDescriptors()) {
                if (descriptor is FunctionDescriptor) {
                    val propertyName = SyntheticExtensionPropertyDescriptor.propertyNameByGetMethodName(descriptor.getName()) ?: continue
                    addIfNotNull(syntheticPropertyInClass(Triple(classifier, type, propertyName)))
                }
            }
        }

        typeConstructor.getSupertypes().forEach { collectSyntheticProperties(it) }
    }

    private fun SmartList<PropertyDescriptor>?.add(property: PropertyDescriptor?): SmartList<PropertyDescriptor>? {
        if (property == null) return this
        val list = if (this != null) this else SmartList()
        list.add(property)
        return list
    }

    //TODO: reuse code with generation?

    private fun possibleGetMethodNames(propertyName: Name): Collection<Name> {
        val capitalized = propertyName.getIdentifier().capitalize()
        return listOf(Name.identifier("get" + capitalized), Name.identifier("is" + capitalized))
    }

    private fun possibleSetMethodName(propertyName: Name): Name {
        return Name.identifier("set" + propertyName.getIdentifier().capitalize())
    }

    private class MyPropertyDescriptor(
            javaClass: JavaClassDescriptor,
            override val getMethod: FunctionDescriptor,
            override val setMethod: FunctionDescriptor?,
            name: Name,
            type: JetType,
            receiverType: JetType
    ) : SyntheticExtensionPropertyDescriptor, PropertyDescriptorImpl(
            DescriptorUtils.getContainingModule(javaClass)/* TODO:is it ok? */,
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
            setType(type, emptyList(), null, receiverType)

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