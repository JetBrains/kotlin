/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorBase
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.ClassTypeConstructorImpl
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

class NotFoundClasses(private val storageManager: StorageManager, private val module: ModuleDescriptor) {
    /**
     * @param typeParametersCount list of numbers of type parameters in this class and all its outer classes, starting from this class
     */
    private data class ClassRequest(val classId: ClassId, val typeParametersCount: List<Int>)

    private val packageFragments = storageManager.createMemoizedFunction<FqName, PackageFragmentDescriptor> { fqName ->
        EmptyPackageFragmentDescriptor(module, fqName)
    }

    private val classes = storageManager.createMemoizedFunction<ClassRequest, ClassDescriptor> { (classId, typeParametersCount) ->
        if (classId.isLocal) {
            throw UnsupportedOperationException("Unresolved local class: $classId")
        }

        val container = classId.outerClassId?.let { outerClassId ->
            getClass(outerClassId, typeParametersCount.drop(1))
        } ?: packageFragments(classId.packageFqName)

        // Treat a class with a nested ClassId as inner for simplicity, otherwise the outer type cannot have generic arguments
        val isInner = classId.isNestedClass

        MockClassDescriptor(storageManager, container, classId.shortClassName, isInner, typeParametersCount.firstOrNull() ?: 0)
    }

    class MockClassDescriptor internal constructor(
            storageManager: StorageManager,
            container: DeclarationDescriptor,
            name: Name,
            private val isInner: Boolean,
            numberOfDeclaredTypeParameters: Int
    ) : ClassDescriptorBase(storageManager, container, name, SourceElement.NO_SOURCE, /* isExternal = */ false) {
        private val declaredTypeParameters = (0 until numberOfDeclaredTypeParameters).map { index ->
            TypeParameterDescriptorImpl.createWithDefaultBound(
                    this, Annotations.EMPTY, false, Variance.INVARIANT, Name.identifier("T$index"), index, storageManager
            )
        }

        private val typeConstructor =
            ClassTypeConstructorImpl(this, computeConstructorTypeParameters(), setOf(module.builtIns.anyType), storageManager)

        override fun getKind() = ClassKind.CLASS
        override fun getModality() = Modality.FINAL
        override fun getVisibility() = DescriptorVisibilities.PUBLIC
        override fun getTypeConstructor() = typeConstructor
        override fun getDeclaredTypeParameters() = declaredTypeParameters
        override fun isInner() = isInner

        override fun isCompanionObject() = false
        override fun isData() = false
        override fun isInline() = false
        override fun isFun() = false
        override fun isValue() = false
        override fun isExpect() = false
        override fun isActual() = false
        override fun isExternal() = false
        override val annotations: Annotations get() = Annotations.EMPTY

        override fun getUnsubstitutedMemberScope(kotlinTypeRefiner: KotlinTypeRefiner) = MemberScope.Empty
        override fun getStaticScope() = MemberScope.Empty
        override fun getConstructors(): Collection<ClassConstructorDescriptor> = emptySet()
        override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? = null
        override fun getCompanionObjectDescriptor(): ClassDescriptor? = null
        override fun getSealedSubclasses(): Collection<ClassDescriptor> = emptyList()
        override fun getInlineClassRepresentation(): InlineClassRepresentation<SimpleType>? = null
        override fun getMultiFieldValueClassRepresentation(): MultiFieldValueClassRepresentation<SimpleType>? = null

        override fun toString() = "class $name (not found)"
    }

    // We create different ClassDescriptor instances for types with the same ClassId but different number of type arguments.
    // (This may happen when a class with the same FQ name is instantiated with different type arguments in different modules.)
    // It's better than creating just one descriptor because otherwise would fail in multiple places where it's asserted that
    // the number of type arguments in a type must be equal to the number of the type parameters of the class
    fun getClass(classId: ClassId, typeParametersCount: List<Int>): ClassDescriptor {
        return classes(ClassRequest(classId, typeParametersCount))
    }
}
