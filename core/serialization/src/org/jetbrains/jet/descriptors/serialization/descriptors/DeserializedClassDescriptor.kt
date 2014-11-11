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
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.StaticScopeForKotlinClass
import org.jetbrains.jet.lang.types.AbstractClassTypeConstructor
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.storage.MemoizedFunctionToNullable

import java.util.*

import org.jetbrains.jet.descriptors.serialization
import org.jetbrains.jet.lang.resolve.name.SpecialNames.getClassObjectName
import org.jetbrains.jet.descriptors.serialization.classKind
import org.jetbrains.jet.utils.addIfNotNull
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.scopes.DescriptorKindFilter

public fun DeserializedClassDescriptor(globalContext: DeserializationGlobalContext, classData: ClassData): DeserializedClassDescriptor
        = DeserializedClassDescriptor(globalContext.withNameResolver(classData.getNameResolver()), classData.getClassProto())

public class DeserializedClassDescriptor(outerContext: DeserializationContext, private val classProto: ProtoBuf.Class)
  : AbstractClassDescriptor(outerContext.storageManager, outerContext.nameResolver.getClassId(classProto.getFqName()).getRelativeClassName().shortName()), ClassDescriptor {

    private val modality = serialization.modality(Flags.MODALITY.get(classProto.getFlags()))
    private val visibility = serialization.visibility(Flags.VISIBILITY.get(classProto.getFlags()))
    private val kind = classKind(Flags.CLASS_KIND.get(classProto.getFlags()))
    private val isInner = Flags.INNER.get(classProto.getFlags())

    private val classId = outerContext.nameResolver.getClassId(classProto.getFqName())
    private val typeParameters = ArrayList<TypeParameterDescriptor>(classProto.getTypeParameterCount())
    private val context = outerContext.withTypes(this).childContext(this, classProto.getTypeParameterList(), typeParameters)

    private val staticScope = StaticScopeForKotlinClass(this)
    private val typeConstructor = DeserializedClassTypeConstructor(typeParameters)
    private val memberScope = DeserializedClassMemberScope()
    private val nestedClasses = NestedClassDescriptors()

    private val containingDeclaration = outerContext.storageManager.createLazyValue { computeContainingDeclaration() }
    private val annotations = context.storageManager.createLazyValue { computeAnnotations() }
    private val primaryConstructor = context.storageManager.createNullableLazyValue { computePrimaryConstructor() }
    private val classObjectDescriptor = context.storageManager.createNullableLazyValue { computeClassObjectDescriptor() }

    override fun getContainingDeclaration(): DeclarationDescriptor = containingDeclaration()

    private fun computeContainingDeclaration(): DeclarationDescriptor {
        if (classId.isTopLevelClass()) {
            val fragments = context.packageFragmentProvider.getPackageFragments(classId.getPackageFqName())
            assert(fragments.size() == 1) { "there should be exactly one package: $fragments, class id is $classId" }
            return fragments.single()
        }
        else {
            return context.deserializeClass(classId.getOuterClassId()) ?: ErrorUtils.getErrorModule()
        }
    }

    override fun getTypeConstructor() = typeConstructor

    override fun getKind() = kind

    override fun getModality() = modality

    override fun getVisibility() = visibility

    override fun isInner() = isInner

    private fun computeAnnotations(): Annotations {
        if (!Flags.HAS_ANNOTATIONS.get(classProto.getFlags())) {
            return Annotations.EMPTY
        }
        return context.annotationLoader.loadClassAnnotations(this, classProto)
    }

    override fun getAnnotations(): Annotations = annotations()

    override fun getScopeForMemberLookup() = memberScope

    override fun getStaticScope() = staticScope

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

    override fun getUnsubstitutedPrimaryConstructor(): ConstructorDescriptor? = primaryConstructor()

    override fun getConstructors(): Collection<ConstructorDescriptor> {
        val constructor = getUnsubstitutedPrimaryConstructor() ?: return listOf()
        // TODO: other constructors
        return listOf(constructor)
    }

    private fun computeClassObjectDescriptor(): ClassDescriptor? {
        if (!classProto.hasClassObject()) return null

        if (getKind() == ClassKind.OBJECT) {
            val classObjectProto = classProto.getClassObject()
            if (!classObjectProto.hasData()) {
                throw IllegalStateException("Object should have a serialized class object: $classId")
            }

            return DeserializedClassDescriptor(context, classObjectProto.getData())
        }

        return context.deserializeClass(classId.createNestedClassId(getClassObjectName(getName())))
    }

    override fun getClassObjectDescriptor(): ClassDescriptor? = classObjectDescriptor()

    private fun computeSuperTypes(): Collection<JetType> {
        val supertypes = ArrayList<JetType>(classProto.getSupertypeCount())
        for (supertype in classProto.getSupertypeList()) {
            supertypes.add(context.typeDeserializer.`type`(supertype))
        }
        return supertypes
    }

    override fun toString() = "deserialized class ${getName().toString()}" // not using descriptor render to preserve laziness

    override fun getSource() = SourceElement.NO_SOURCE

    private inner class DeserializedClassTypeConstructor(private val parameters: List<TypeParameterDescriptor>) : AbstractClassTypeConstructor() {
        private val supertypes = computeSuperTypes()

        override fun getParameters() = parameters

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

        override fun isFinal() = !getModality().isOverridable()

        override fun isDenotable() = true

        override fun getDeclarationDescriptor() = this@DeserializedClassDescriptor

        override fun getAnnotations(): Annotations = Annotations.EMPTY // TODO

        override fun toString() = getName().toString()
    }

    private inner class DeserializedClassMemberScope : DeserializedMemberScope(context, this@DeserializedClassDescriptor.classProto.getMemberList()) {
        private val classDescriptor: DeserializedClassDescriptor = this@DeserializedClassDescriptor
        private val allDescriptors = context.storageManager.createLazyValue { computeDescriptors(DescriptorKindFilter.ALL, JetScope.ALL_NAME_FILTER) }

        override fun getDescriptors(kindFilter: DescriptorKindFilter,
                                    nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> = allDescriptors()

        override fun computeNonDeclaredFunctions(name: Name, functions: MutableCollection<FunctionDescriptor>) {
            val fromSupertypes = ArrayList<FunctionDescriptor>()
            for (supertype in classDescriptor.getTypeConstructor().getSupertypes()) {
                fromSupertypes.addAll(supertype.getMemberScope().getFunctions(name))
            }
            generateFakeOverrides(name, fromSupertypes, functions)
        }

        override fun computeNonDeclaredProperties(name: Name, descriptors: MutableCollection<PropertyDescriptor>) {
            val fromSupertypes = ArrayList<PropertyDescriptor>()
            for (supertype in classDescriptor.getTypeConstructor().getSupertypes()) {
                [suppress("UNCHECKED_CAST")]
                fromSupertypes.addAll(supertype.getMemberScope().getProperties(name) as Collection<PropertyDescriptor>)
            }
            generateFakeOverrides(name, fromSupertypes, descriptors)
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

        override fun getImplicitReceiver() = classDescriptor.getThisAsReceiverParameter()

        override fun getClassDescriptor(name: Name): ClassifierDescriptor? = classDescriptor.nestedClasses.findClass(name)

        override fun addClassDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean) {
            result.addAll(classDescriptor.nestedClasses.getAllDescriptors())
        }
    }

    private inner class NestedClassDescriptors {
        private val nestedClassNames = nestedClassNames()
        private val enumEntryNames = enumEntryNames()

        val findClass: MemoizedFunctionToNullable<Name, ClassDescriptor> = run {
            val storageManager = context.storageManager
            val enumMemberNames = storageManager.createLazyValue { computeEnumMemberNames() }

            storageManager.createMemoizedFunctionWithNullableValues<Name, ClassDescriptor> { name ->
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
            val result = LinkedHashSet<Name>()
            val nameResolver = context.nameResolver
            for (index in classProto.getNestedClassNameList()) {
                result.add(nameResolver.getName(index!!))
            }
            return result
        }

        private fun enumEntryNames(): Set<Name> {
            if (getKind() != ClassKind.ENUM_CLASS) return setOf()

            val result = LinkedHashSet<Name>()
            val nameResolver = context.nameResolver
            for (index in classProto.getEnumEntryList()) {
                result.add(nameResolver.getName(index!!))
            }
            return result
        }

        private fun computeEnumMemberNames(): Collection<Name> {
            // NOTE: order of enum entry members should be irrelevant
            // because enum entries are effectively invisible to user (as classes)
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
                result.addIfNotNull(findClass(name))
            }
            for (name in enumEntryNames) {
                result.addIfNotNull(findClass(name))
            }
            return result
        }
    }
}
