/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AbstractTypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorBase
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.types.*

class NotFoundClasses(private val storageManager: StorageManager, private val module: ModuleDescriptor) {
    /**
     * @param typeParametersCount list of numbers of type parameters in this class and all its outer classes, starting from this class
     */
    private data class ClassRequest(val classId: ClassId, val typeParametersCount: List<Int>)

    private val packageFragments = storageManager.createMemoizedFunction<FqName, PackageFragmentDescriptor> { fqName ->
        EmptyPackageFragmentDescriptor(module, fqName)
    }

    private val classes = storageManager.createMemoizedFunction<ClassRequest, ClassDescriptor> { request ->
        computeClassifier(request, {
            owner, name, isInner, numberOfTypeParametersCount ->
            MockClassDescriptor(storageManager, owner, name, isInner, numberOfTypeParametersCount)
        })
    }

    private val typeAliases = storageManager.createMemoizedFunction<ClassRequest, TypeAliasDescriptor> { request ->
        computeClassifier(request, {
            owner, name, isInner, numberOfTypeParametersCount ->
            MockTypeAliasDescriptor(storageManager, owner, name, isInner, numberOfTypeParametersCount)
        })
    }

    // TODO: Uncomment this when KT-12871 is fixed
    // private typealias ConstructorFunction<D> = (DeclarationDescriptor, Name, isInner: Boolean, numberOfTypeParametersCount: Int) -> D
    private fun <D> computeClassifier(
            request: ClassRequest,
            constructor: (DeclarationDescriptor, Name, isInner: Boolean, numberOfTypeParametersCount: Int) -> D
    ): D {
        val (classId, typeParametersCount) = request

        if (classId.isLocal) {
            throw UnsupportedOperationException("Unresolved local class: $classId")
        }

        val container =
                if (classId.isNestedClass) getOrCreateClass(classId.outerClassId, typeParametersCount.drop(1))
                else packageFragments(classId.packageFqName)

        // Treat a class with a nested ClassId as inner for simplicity, otherwise the outer type cannot have generic arguments
        val isInner = classId.isNestedClass

        return constructor(container, classId.shortClassName, isInner, typeParametersCount.firstOrNull() ?: 0)
    }

    class MockClassDescriptor internal constructor(
            storageManager: StorageManager,
            container: DeclarationDescriptor,
            name: Name,
            private val isInner: Boolean,
            numberOfDeclaredTypeParameters: Int
    ) : ClassDescriptorBase(storageManager, container, name, SourceElement.NO_SOURCE) {
        private val typeParameters = createTypeParameters(this, numberOfDeclaredTypeParameters)

        private val typeConstructor = ClassTypeConstructorImpl(this, /* isFinal = */ true, typeParameters, setOf(module.builtIns.anyType))

        override fun getKind() = ClassKind.CLASS
        override fun getModality() = Modality.FINAL
        override fun getVisibility() = Visibilities.PUBLIC
        override fun getTypeConstructor() = typeConstructor
        override fun getDeclaredTypeParameters() = typeParameters
        override fun isInner() = isInner

        override fun isCompanionObject() = false
        override fun isData() = false
        override val annotations: Annotations get() = Annotations.EMPTY

        override fun getUnsubstitutedMemberScope() = MemberScope.Empty
        override fun getStaticScope() = MemberScope.Empty
        override fun getConstructors(): Collection<ClassConstructorDescriptor> = emptySet()
        override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? = null
        override fun getCompanionObjectDescriptor(): ClassDescriptor? = null

        override fun toString() = "class $name (not found)"
    }

    private class MockTypeAliasDescriptor(
            storageManager: StorageManager,
            containingDeclaration: DeclarationDescriptor,
            name: Name,
            private val isInner: Boolean,
            numberOfDeclaredTypeParameters: Int
    ) : AbstractTypeAliasDescriptor(containingDeclaration, Annotations.EMPTY, name, SourceElement.NO_SOURCE, Visibilities.PUBLIC) {
        init {
            initialize(createTypeParameters(this, numberOfDeclaredTypeParameters))
        }

        private val constructorTypeParameters by storageManager.createLazyValue { computeConstructorTypeParameters() }

        override fun getTypeConstructorTypeParameters() = constructorTypeParameters

        // We don't have enough information about underlying type, so just take nullable Any?
        // Anyway it should not used extensively, because not found type aliases are only used for type abbreviations
        override val underlyingType: SimpleType
            get() = builtIns.nullableAnyType
        override val expandedType: SimpleType
            get() = builtIns.nullableAnyType
        override fun getDefaultType(): SimpleType =
                builtIns.nullableAnyType
        override val classDescriptor: ClassDescriptor?
            get() = expandedType.constructor.declarationDescriptor as? ClassDescriptor

        override fun isInner(): Boolean = isInner

        override fun substitute(substitutor: TypeSubstitutor) = this

        override fun toString() = "MockTypeAliasDescriptor[$fqNameUnsafe]"
    }

    // We create different ClassDescriptor instances for types with the same ClassId but different number of type arguments.
    // (This may happen when a class with the same FQ name is instantiated with different type arguments in different modules.)
    // It's better than creating just one descriptor because otherwise would fail in multiple places where it's asserted that
    // the number of type arguments in a type must be equal to the number of the type parameters of the class
    private fun getOrCreateClass(classId: ClassId, typeParametersCount: List<Int>): ClassDescriptor {
        return classes(ClassRequest(classId, typeParametersCount))
    }

    fun getClass(proto: ProtoBuf.Type, nameResolver: NameResolver, typeTable: TypeTable): TypeConstructor {
        val classId = nameResolver.getClassId(proto.className)
        return getOrCreateClass(classId, computeTypeParametersCount(classId, proto, typeTable)).typeConstructor
    }

    fun getClass(classId: ClassId, typeParametersCount: List<Int>): TypeConstructor {
        return getOrCreateClass(classId, typeParametersCount).typeConstructor
    }

    fun getTypeAlias(proto: ProtoBuf.Type, nameResolver: NameResolver, typeTable: TypeTable): TypeConstructor {
        val classId = nameResolver.getClassId(proto.typeAliasName)
        return typeAliases(ClassRequest(classId, computeTypeParametersCount(classId, proto, typeTable))).typeConstructor
    }
}

private fun createTypeParameters(
        classifierDescriptor: ClassifierDescriptor,
        numberOfDeclaredTypeParameters: Int
) = (1..numberOfDeclaredTypeParameters).map { index ->
    TypeParameterDescriptorImpl.createWithDefaultBound(
            classifierDescriptor, Annotations.EMPTY, false, Variance.INVARIANT, Name.identifier("T$index"), index
    )
}

private fun computeTypeParametersCount(classId: ClassId, proto: ProtoBuf.Type, typeTable: TypeTable): List<Int> {
    val typeParametersCount = generateSequence(proto) { it.outerType(typeTable) }.map { it.argumentCount }.toMutableList()
    val classNestingLevel = generateSequence(classId) { if (it.isNestedClass) it.outerClassId else null }.count()
    while (typeParametersCount.size < classNestingLevel) {
        typeParametersCount.add(0)
    }
    return typeParametersCount
}
