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
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import org.jetbrains.jet.lang.resolve.name.SpecialNames
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.DescriptorFactory
import org.jetbrains.jet.lang.types.TypeConstructor
import org.jetbrains.jet.lang.types.TypeConstructorImpl
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.OverridingUtil
import org.jetbrains.jet.lang.resolve.scopes.WritableScope.LockLevel
import org.jetbrains.jet.lang.types.DelegatingType

public class EnumClassObjectDescriptor(
        storageManager: StorageManager,
        enumClass: ClassDescriptor
) : ClassDescriptorBase(storageManager, enumClass, SpecialNames.getClassObjectName(enumClass.getName())) {
    private val primaryConstructor = DescriptorFactory.createPrimaryConstructorForObject(this)

    ;{
        primaryConstructor.setReturnType(object : DelegatingType() {
            override fun getDelegate() = getDefaultType()
        })
    }

    private val _typeConstructor = TypeConstructorImpl.createForClass(this, getAnnotations(), true, getName().asString(),
                                                                      listOf(), listOf(KotlinBuiltIns.getInstance().getAnyType()))

    private val scope = WritableScopeImpl(JetScope.EMPTY, this, RedeclarationHandler.DO_NOTHING, "MemberLookup")

    ;{
        val enumType = (getContainingDeclaration() as ClassDescriptor).getDefaultType()
        val enumArrayType = KotlinBuiltIns.getInstance().getArrayType(enumType)
        scope.addFunctionDescriptor(DescriptorFactory.createEnumClassObjectValuesMethod(this, enumArrayType))
        scope.addFunctionDescriptor(DescriptorFactory.createEnumClassObjectValueOfMethod(this, enumType))

        val sink = object : OverridingUtil.DescriptorSink {
            override fun addToScope(fakeOverride: CallableMemberDescriptor) {
                OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null)
                scope.addFunctionDescriptor(fakeOverride as SimpleFunctionDescriptor)
            }

            override fun conflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
                throw IllegalStateException("Conflict on enum class object override: $fromSuper vs $fromCurrent")
            }
        }

        val superScope = KotlinBuiltIns.getInstance().getAnyType().getMemberScope()

        for (descriptor in superScope.getAllDescriptors()) {
            if (descriptor is FunctionDescriptor) {
                val name = descriptor.getName()
                OverridingUtil.generateOverridesInFunctionGroup(name, superScope.getFunctions(name), setOf(), this, sink)
            }
        }

        scope.changeLockLevel(LockLevel.READING)
    }

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
}
