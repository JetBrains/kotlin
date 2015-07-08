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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
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
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

interface SyntheticExtensionPropertyDescriptor : PropertyDescriptor {
    val getMethod: FunctionDescriptor
    val setMethod: FunctionDescriptor?
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
        val firstChar = name.getIdentifier()[0]
        if (!firstChar.isJavaIdentifierStart() || firstChar.isUpperCase()) return null

        val memberScope = javaClass.getMemberScope(type.getArguments())
        val getMethod = memberScope.getFunctions(toGetMethodName(name)).singleOrNull { isGoodGetMethod(it) } ?: return null

        val propertyType = getMethod.getReturnType() ?: return null
        val setMethod = memberScope.getFunctions(toSetMethodName(name)).singleOrNull { isGoodSetMethod(it, propertyType) }

        return MyPropertyDescriptor(javaClass, getMethod, setMethod, name, propertyType, type)
    }

    private fun isGoodGetMethod(descriptor: FunctionDescriptor): Boolean {
        return descriptor.getValueParameters().isEmpty()
               && descriptor.getTypeParameters().isEmpty()
               && descriptor.getVisibility() == Visibilities.PUBLIC //TODO: what about protected and package-local?
    }

    private fun isGoodSetMethod(descriptor: FunctionDescriptor, propertyType: JetType): Boolean {
        return descriptor.getValueParameters().singleOrNull()?.getType() == propertyType
               && descriptor.getTypeParameters().isEmpty()
               && descriptor.getReturnType()?.let { KotlinBuiltIns.isUnit(it) } ?: false
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
                    val propertyName = fromGetMethodName(descriptor.getName()) ?: continue
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

    //TODO: "is"?
    //TODO: methods like "getURL"?
    //TODO: reuse code with generation?
    private fun toGetMethodName(propertyName: Name): Name {
        return Name.identifier("get" + propertyName.getIdentifier().capitalize())
    }

    private fun toSetMethodName(propertyName: Name): Name {
        return Name.identifier("set" + propertyName.getIdentifier().capitalize())
    }

    private fun fromGetMethodName(methodName: Name): Name? {
        if (methodName.isSpecial()) return null
        val identifier = methodName.getIdentifier()
        if (!identifier.startsWith("get")) return null
        val name = identifier.removePrefix("get").decapitalize()
        if (!Name.isValidIdentifier(name)) return null
        return Name.identifier(name)
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