/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.descriptors.impl

import org.jetbrains.jet.storage.StorageManager
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import org.jetbrains.jet.lang.resolve.name.SpecialNames
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.DescriptorFactory
import org.jetbrains.jet.lang.types.TypeConstructor
import org.jetbrains.jet.lang.types.TypeConstructorImpl
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.OverridingUtil
import org.jetbrains.jet.lang.types.DelegatingType
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScopeImpl
import org.jetbrains.jet.utils.Printer
import java.util.ArrayList
import kotlin.properties.Delegates

public class EnumClassObjectDescriptor(
        storageManager: StorageManager,
        enumClass: ClassDescriptor
) : ClassDescriptorBase(storageManager, enumClass, SpecialNames.getClassObjectName(enumClass.getName()), SourceElement.NO_SOURCE) {
    private val primaryConstructor = DescriptorFactory.createPrimaryConstructorForObject(this, SourceElement.NO_SOURCE)

    ;{
        primaryConstructor.setReturnType(object : DelegatingType() {
            override fun getDelegate() = getDefaultType()
        })
    }

    private val _typeConstructor = TypeConstructorImpl.createForClass(this, getAnnotations(), true, getName().asString(),
                                                                      listOf(), listOf(KotlinBuiltIns.getInstance().getAnyType()))

    private val scope = EnumClassObjectScope()

    override fun getScopeForMemberLookup(): JetScope = scope

    override fun getClassObjectDescriptor(): ClassDescriptor? = null

    override fun getConstructors(): List<ConstructorDescriptor> = listOf(primaryConstructor)

    override fun getUnsubstitutedPrimaryConstructor(): ConstructorDescriptor = primaryConstructor

    override fun getTypeConstructor(): TypeConstructor = _typeConstructor

    override fun getKind(): ClassKind = ClassKind.CLASS_OBJECT

    override fun getModality(): Modality = Modality.FINAL

    override fun getVisibility(): Visibility = DescriptorUtils.getSyntheticClassObjectVisibility()

    override fun isInner(): Boolean = false

    override fun getAnnotations(): Annotations = Annotations.EMPTY

    private inner class EnumClassObjectScope : JetScopeImpl() {
        private val enumClassObject: EnumClassObjectDescriptor
            get() = this@EnumClassObjectDescriptor

        private val functions: List<FunctionDescriptor> by Delegates.lazy {
            val result = ArrayList<FunctionDescriptor>(5)

            val enumType = object : DelegatingType() {
                override fun getDelegate() = (enumClassObject.getContainingDeclaration() as ClassDescriptor).getDefaultType()
            }

            result.add(createEnumClassObjectValuesMethod(enumType))
            result.add(createEnumClassObjectValueOfMethod(enumType))

            val sink = object : OverridingUtil.DescriptorSink {
                override fun addToScope(fakeOverride: CallableMemberDescriptor) {
                    OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null)
                    result.add(fakeOverride as FunctionDescriptor)
                }

                override fun conflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
                    throw IllegalStateException("Conflict on enum class object override: $fromSuper vs $fromCurrent")
                }
            }

            val superScope = KotlinBuiltIns.getInstance().getAnyType().getMemberScope()

            for (descriptor in superScope.getAllDescriptors()) {
                if (descriptor is FunctionDescriptor) {
                    val name = descriptor.getName()
                    OverridingUtil.generateOverridesInFunctionGroup(name, superScope.getFunctions(name), setOf(), enumClassObject, sink)
                }
            }

            result
        }

        private fun createEnumClassObjectValuesMethod(enumType: JetType): SimpleFunctionDescriptor {
            val enumArrayType = KotlinBuiltIns.getInstance().getArrayType(enumType)
            val values = SimpleFunctionDescriptorImpl.create(enumClassObject, Annotations.EMPTY, Name.identifier("values"), SYNTHESIZED, SourceElement.NO_SOURCE)
            return values.initialize(null, getThisAsReceiverParameter(), listOf(), listOf(), enumArrayType, Modality.FINAL, Visibilities.PUBLIC)
        }

        private fun createEnumClassObjectValueOfMethod(enumType: JetType): SimpleFunctionDescriptor {
            val values = SimpleFunctionDescriptorImpl.create(enumClassObject, Annotations.EMPTY, Name.identifier("valueOf"), SYNTHESIZED, SourceElement.NO_SOURCE)
            val parameter = ValueParameterDescriptorImpl(values, null, 0, Annotations.EMPTY, Name.identifier("value"),
                                                         KotlinBuiltIns.getInstance().getStringType(), false, null, SourceElement.NO_SOURCE)
            return values.initialize(null, getThisAsReceiverParameter(), listOf(), listOf(parameter), enumType, Modality.FINAL, Visibilities.PUBLIC)
        }

        override fun getFunctions(name: Name) = functions.filter { it.getName() == name }

        override fun getAllDescriptors() = functions

        override fun printScopeStructure(p: Printer) {
            p.println("enum class object scope for $enumClassObject")
        }

        override fun getContainingDeclaration(): ClassDescriptor = enumClassObject
    }
}
