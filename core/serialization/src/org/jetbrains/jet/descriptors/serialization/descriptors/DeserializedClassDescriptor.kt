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

package org.jetbrains.jet.descriptors.serialization.descriptors

import org.jetbrains.jet.descriptors.serialization.ClassData
import org.jetbrains.jet.descriptors.serialization.Flags
import org.jetbrains.jet.descriptors.serialization.ProtoBuf
import org.jetbrains.jet.descriptors.serialization.context.*
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import org.jetbrains.jet.lang.descriptors.impl.AbstractClassDescriptor
import org.jetbrains.jet.lang.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorFactory
import org.jetbrains.jet.lang.resolve.OverridingUtil
import org.jetbrains.jet.lang.resolve.name.ClassId
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.scopes.StaticScopeForKotlinClass
import org.jetbrains.jet.lang.types.AbstractClassTypeConstructor
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.TypeConstructor
import org.jetbrains.jet.storage.MemoizedFunctionToNullable
import org.jetbrains.jet.storage.NotNullLazyValue
import org.jetbrains.jet.storage.NullableLazyValue

import java.util.*

import org.jetbrains.jet.descriptors.serialization
import org.jetbrains.jet.lang.resolve.name.SpecialNames.getClassObjectName
import org.jetbrains.jet.descriptors.serialization.classKind


public fun DeserializedClassDescriptor(globalContext: DeserializationGlobalContext, classData: ClassData): DeserializedClassDescriptor {
    return DeserializedClassDescriptor(globalContext.withNameResolver(classData.getNameResolver()), classData.getClassProto())
}

public class DeserializedClassDescriptor(outerContext: DeserializationContext, private val classProto: ProtoBuf.Class) : AbstractClassDescriptor(outerContext.storageManager, outerContext.nameResolver.getClassId(classProto.getFqName()).getRelativeClassName().shortName()), ClassDescriptor {

    private val classId: ClassId
    private val memberScope: DeserializedMemberScope

    private val primaryConstructor: NullableLazyValue<ConstructorDescriptor>

    private val annotations: NotNullLazyValue<Annotations>

    private val classObjectDescriptor: NullableLazyValue<ClassDescriptor>
    private val nestedClasses: NestedClassDescriptors
    private val staticScope = StaticScopeForKotlinClass(this)

    private val containingDeclaration: NotNullLazyValue<DeclarationDescriptor>
    private val typeConstructor: DeserializedClassTypeConstructor
    private val modality: Modality
    private val visibility: Visibility
    private val kind: ClassKind
    private val isInner: Boolean
    private val context: DeserializationContextWithTypes

    {
        this.classId = outerContext.nameResolver.getClassId(classProto.getFqName())

        val typeParameters = ArrayList<TypeParameterDescriptor>(classProto.getTypeParameterCount())
        this.context = outerContext.withTypes(this).childContext(this, classProto.getTypeParameterList(), typeParameters)

        this.containingDeclaration = outerContext.storageManager.createLazyValue<DeclarationDescriptor>(object : Function0<DeclarationDescriptor> {
            override fun invoke(): DeclarationDescriptor {
                return computeContainingDeclaration()
            }
        })

        this.typeConstructor = DeserializedClassTypeConstructor(typeParameters)
        this.memberScope = DeserializedClassMemberScope()

        val flags = classProto.getFlags()
        this.modality = serialization.modality(Flags.MODALITY.get(flags))
        this.visibility = serialization.visibility(Flags.VISIBILITY.get(flags))
        this.kind = classKind(Flags.CLASS_KIND.get(flags))
        this.isInner = Flags.INNER.get(flags)

        this.annotations = context.storageManager.createLazyValue<Annotations> { computeAnnotations() }

        this.primaryConstructor = context.storageManager.createNullableLazyValue<ConstructorDescriptor> { computePrimaryConstructor() }

        this.classObjectDescriptor = context.storageManager.createNullableLazyValue<ClassDescriptor> { computeClassObjectDescriptor() }

        this.nestedClasses = NestedClassDescriptors()
    }

    override fun getContainingDeclaration(): DeclarationDescriptor {
        return containingDeclaration.invoke()
    }

    private fun computeContainingDeclaration(): DeclarationDescriptor {
        if (classId.isTopLevelClass()) {
            val fragments = context.packageFragmentProvider.getPackageFragments(classId.getPackageFqName())
            assert(fragments.size() == 1) { "there should be exactly one package: " + fragments + ", class id is " + classId }
            return fragments.iterator().next()
        }
        else {
            return context.deserializeClass(classId.getOuterClassId()) ?: ErrorUtils.getErrorModule()
        }
    }

    override fun getTypeConstructor(): TypeConstructor {
        return typeConstructor
    }


    override fun getKind(): ClassKind {
        return kind
    }

    override fun getModality(): Modality {
        return modality
    }

    override fun getVisibility(): Visibility {
        return visibility
    }

    override fun isInner(): Boolean {
        return isInner
    }

    private fun computeAnnotations(): Annotations {
        if (!Flags.HAS_ANNOTATIONS.get(classProto.getFlags())) {
            return Annotations.EMPTY
        }
        return context.annotationLoader.loadClassAnnotations(this, classProto)
    }

    override fun getAnnotations(): Annotations {
        return annotations.invoke()
    }

    override fun getScopeForMemberLookup(): JetScope {
        return memberScope
    }

    override fun getStaticScope(): JetScope {
        return staticScope
    }

    private fun computePrimaryConstructor(): ConstructorDescriptor? {
        if (!classProto.hasPrimaryConstructor()) return null

        val constructorProto = classProto.getPrimaryConstructor()
        if (!constructorProto.hasData()) {
            val descriptor = DescriptorFactory.createPrimaryConstructorForObject(this, SourceElement.NO_SOURCE)
            descriptor.setReturnType(getDefaultType())
            return descriptor
        }

        return context.deserializer.loadCallable(constructorProto.getData()) as ConstructorDescriptor
    }

    override fun getUnsubstitutedPrimaryConstructor(): ConstructorDescriptor? {
        return primaryConstructor.invoke()
    }

    override fun getConstructors(): Collection<ConstructorDescriptor> {
        val constructor = getUnsubstitutedPrimaryConstructor()
        if (constructor == null) {
            return listOf()
        }
        // TODO: other constructors
        return listOf(constructor)
    }

    private fun computeClassObjectDescriptor(): ClassDescriptor? {
        if (!classProto.hasClassObject()) {
            return null
        }

        if (getKind() == ClassKind.OBJECT) {
            val classObjectProto = classProto.getClassObject()
            if (!classObjectProto.hasData()) {
                throw IllegalStateException("Object should have a serialized class object: " + classId)
            }

            return DeserializedClassDescriptor(context, classObjectProto.getData())
        }

        return context.deserializeClass(classId.createNestedClassId(getClassObjectName(getName())))
    }

    override fun getClassObjectDescriptor(): ClassDescriptor? {
        return classObjectDescriptor.invoke()
    }

    private fun computeSuperTypes(): Collection<JetType> {
        val supertypes = ArrayList<JetType>(classProto.getSupertypeCount())
        for (supertype in classProto.getSupertypeList()) {
            supertypes.add(context.typeDeserializer.`type`(supertype))
        }
        return supertypes
    }

    override fun toString(): String {
        // not using descriptor render to preserve laziness
        return "deserialized class " + getName().toString()
    }

    override fun getSource(): SourceElement {
        return SourceElement.NO_SOURCE
    }

    private inner class DeserializedClassTypeConstructor(private val parameters: List<TypeParameterDescriptor>) : AbstractClassTypeConstructor() {
        private val supertypes = computeSuperTypes()

        override fun getParameters(): List<TypeParameterDescriptor> {
            return parameters
        }

        override fun getSupertypes(): Collection<JetType> {
            // We cannot have error supertypes because subclasses inherit error functions from them
            // Filtering right away means copying the list every time, so we check for the rare condition first, and only then filter
            for (supertype in supertypes) {
                if (supertype.isError()) {
                    return supertypes.filter { !it.isError() }
                }
            }
            return supertypes
        }

        override fun isFinal(): Boolean {
            return !getModality().isOverridable()
        }

        override fun isDenotable(): Boolean {
            return true
        }

        override fun getDeclarationDescriptor(): ClassifierDescriptor? {
            return this@DeserializedClassDescriptor
        }

        override fun getAnnotations(): Annotations {
            return Annotations.EMPTY // TODO
        }

        override fun toString(): String {
            return getName().toString()
        }
    }

    private inner class DeserializedClassMemberScope : DeserializedMemberScope(context, this@DeserializedClassDescriptor.classProto.getMemberList()) {
        private val classDescriptor: DeserializedClassDescriptor

        {
            this.classDescriptor = this@DeserializedClassDescriptor
        }

        override fun computeNonDeclaredFunctions(name: Name, functions: MutableCollection<FunctionDescriptor>) {
            val fromSupertypes = ArrayList<FunctionDescriptor>()
            for (supertype in classDescriptor.getTypeConstructor().getSupertypes()) {
                fromSupertypes.addAll(supertype.getMemberScope().getFunctions(name))
            }
            generateFakeOverrides(name, fromSupertypes, functions)
        }

        override fun computeNonDeclaredProperties(name: Name, property: MutableCollection<PropertyDescriptor>) {
            val fromSupertypes = ArrayList<PropertyDescriptor>()
            for (supertype in classDescriptor.getTypeConstructor().getSupertypes()) {
                [suppress("UNCHECKED_CAST")]
                fromSupertypes.addAll(supertype.getMemberScope().getProperties(name) as Collection<PropertyDescriptor>)
            }
            generateFakeOverrides(name, fromSupertypes, property)
        }

        private fun <D : CallableMemberDescriptor> generateFakeOverrides(name: Name, fromSupertypes: Collection<D>, result: MutableCollection<D>) {
            val fromCurrent = ArrayList<CallableMemberDescriptor>(result)
            OverridingUtil.generateOverridesInFunctionGroup(name, fromSupertypes, fromCurrent, classDescriptor, object : OverridingUtil.DescriptorSink {
                override fun addToScope(fakeOverride: CallableMemberDescriptor) {
                    // TODO: report "cannot infer visibility"
                    OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null)
                    [suppress("UNCHECKED_CAST")]
                    result.add(fakeOverride as D)
                }

                override fun conflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
                    // TODO report conflicts
                }
            })
        }

        override fun addNonDeclaredDescriptors(result: MutableCollection<DeclarationDescriptor>) {
            for (supertype in classDescriptor.getTypeConstructor().getSupertypes()) {
                for (descriptor in supertype.getMemberScope().getAllDescriptors()) {
                    if (descriptor is FunctionDescriptor) {
                        result.addAll(getFunctions(descriptor.getName()))
                    }
                    else if (descriptor is PropertyDescriptor) {
                        result.addAll(getProperties(descriptor.getName()))
                    }
                    // Nothing else is inherited
                }
            }
        }

        override fun getImplicitReceiver(): ReceiverParameterDescriptor? {
            return classDescriptor.getThisAsReceiverParameter()
        }

        override fun getClassDescriptor(name: Name): ClassifierDescriptor? {
            return classDescriptor.nestedClasses.findClass.invoke(name)
        }

        override fun addAllClassDescriptors(result: MutableCollection<DeclarationDescriptor>) {
            result.addAll(classDescriptor.nestedClasses.getAllDescriptors())
        }
    }

    private inner class NestedClassDescriptors {
        private val nestedClassNames: Set<Name>
        val findClass: MemoizedFunctionToNullable<Name, ClassDescriptor>
        private val enumEntryNames: Set<Name>

        {
            this.nestedClassNames = nestedClassNames()
            this.enumEntryNames = enumEntryNames()

            val storageManager = context.storageManager
            val enumMemberNames = storageManager.createLazyValue<Collection<Name>>(object : Function0<Collection<Name>> {
                override fun invoke(): Collection<Name> {
                    return computeEnumMemberNames()
                }
            })

            this.findClass = storageManager.createMemoizedFunctionWithNullableValues<Name, ClassDescriptor> { name ->
                if (enumEntryNames.contains(name)) {
                    EnumEntrySyntheticClassDescriptor.create(storageManager, this@DeserializedClassDescriptor, name, enumMemberNames, SourceElement.NO_SOURCE)
                }
                else if (nestedClassNames.contains(name)) {
                    context.deserializeClass(classId.createNestedClassId(name))
                }
                else {
                    null
                }
            }
        }

        private fun nestedClassNames(): Set<Name> {
            val result = HashSet<Name>()
            val nameResolver = context.nameResolver
            for (index in classProto.getNestedClassNameList()) {
                result.add(nameResolver.getName(index!!))
            }
            return result
        }

        private fun enumEntryNames(): Set<Name> {
            if (getKind() != ClassKind.ENUM_CLASS) {
                return setOf()
            }

            val result = HashSet<Name>()
            val nameResolver = context.nameResolver
            for (index in classProto.getEnumEntryList()) {
                result.add(nameResolver.getName(index!!))
            }
            return result
        }

        private fun computeEnumMemberNames(): Collection<Name> {
            val result = HashSet<Name>()

            for (supertype in getTypeConstructor().getSupertypes()) {
                for (descriptor in supertype.getMemberScope().getAllDescriptors()) {
                    if (descriptor is SimpleFunctionDescriptor || descriptor is PropertyDescriptor) {
                        result.add(descriptor.getName())
                    }
                }
            }

            val nameResolver = context.nameResolver
            return classProto.getMemberList().mapTo(result) { nameResolver.getName(it.getName()) }
        }

        public fun getAllDescriptors(): Collection<ClassDescriptor> {
            val result = ArrayList<ClassDescriptor>(nestedClassNames.size() + enumEntryNames.size())
            for (name in nestedClassNames) {
                val descriptor = findClass.invoke(name)
                if (descriptor != null) {
                    result.add(descriptor)
                }
            }
            for (name in enumEntryNames) {
                val descriptor = findClass.invoke(name)
                if (descriptor != null) {
                    result.add(descriptor)
                }
            }
            return result
        }
    }
}
