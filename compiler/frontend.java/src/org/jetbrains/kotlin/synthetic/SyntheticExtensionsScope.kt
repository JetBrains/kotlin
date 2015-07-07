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
        syntheticPropertyInClass(triple.first, triple.second, triple.third)
    }

    private fun syntheticPropertyInClass(javaClass: JavaClassDescriptor, type: JetType, name: Name): PropertyDescriptor? {
        val memberScope = javaClass.getMemberScope(type.getArguments())
        val getMethod = memberScope.getFunctions(name.toGetMethodName()).singleOrNull {
            it.getValueParameters().isEmpty() && it.getTypeParameters().isEmpty() && it.getVisibility() == Visibilities.PUBLIC //TODO: what about protected and package-local?
        } ?: return null

        val propertyType = getMethod.getReturnType() ?: return null
        val setMethod = memberScope.getFunctions(name.toSetMethodName()).singleOrNull {
            it.getValueParameters().singleOrNull()?.getType() == propertyType
            && it.getTypeParameters().isEmpty()
            && it.getReturnType()?.let { KotlinBuiltIns.isUnit(it) } ?: false
            && it.getVisibility() == Visibilities.PUBLIC
        }

        return MyPropertyDescriptor(javaClass, getMethod, setMethod, name, propertyType, type)
    }

    override fun getSyntheticExtensionProperties(receiverType: JetType, name: Name): Collection<VariableDescriptor> {
        if (name.isSpecial()) return emptyList()
        if (name.getIdentifier()[0].isUpperCase()) return emptyList()
        return collectSyntheticProperties(null, receiverType, name) ?: emptyList()
    }

    private fun collectSyntheticProperties(result: SmartList<PropertyDescriptor>?, type: JetType, name: Name): SmartList<PropertyDescriptor>? {
        @suppress("NAME_SHADOWING")
        var result = result

        val typeConstructor = type.getConstructor()
        val classifier = typeConstructor.getDeclarationDescriptor()
        if (classifier is JavaClassDescriptor) {
            result = result.add(syntheticPropertyInClass(Triple(classifier, type, name)))
        }

        typeConstructor.getSupertypes().forEach { result = collectSyntheticProperties(result, it, name) }

        return result
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
    private fun Name.toGetMethodName(): Name {
        return Name.identifier("get" + getIdentifier().capitalize())
    }

    private fun Name.toSetMethodName(): Name {
        return Name.identifier("set" + getIdentifier().capitalize())
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