/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

interface JavaElement

interface JavaNamedElement : JavaElement {
    val name: Name
    val isFromSource: Boolean
}

interface JavaAnnotationOwner : JavaElement {
    val annotations: Collection<JavaAnnotation>
    val isDeprecatedInJavaDoc: Boolean

    fun findAnnotation(fqName: FqName): JavaAnnotation?
}

interface JavaModifierListOwner : JavaElement {
    val isAbstract: Boolean
    val isStatic: Boolean
    val isFinal: Boolean
    val visibility: Visibility
}

interface JavaTypeParameterListOwner : JavaElement {
    val typeParameters: List<JavaTypeParameter>
}

interface JavaAnnotation : JavaElement {
    val arguments: Collection<JavaAnnotationArgument>
    val classId: ClassId?
    val isIdeExternalAnnotation: Boolean
        get() = false
    val isFreshlySupportedTypeUseAnnotation: Boolean
        get() = false

    fun isResolvedTo(fqName: FqName) : Boolean {
        return classId?.asSingleFqName() == fqName
    }

    fun resolve(): JavaClass?
}

interface MapBasedJavaAnnotationOwner : JavaAnnotationOwner {
    val annotationsByFqName: Map<FqName?, JavaAnnotation>

    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName) = annotationsByFqName[fqName]
}

interface ListBasedJavaAnnotationOwner : JavaAnnotationOwner {
    override fun findAnnotation(fqName: FqName) = annotations.find { it.classId?.asSingleFqName() == fqName }
}

interface MutableJavaAnnotationOwner : JavaAnnotationOwner {
    override val annotations: MutableCollection<JavaAnnotation>
}

fun JavaAnnotationOwner.buildLazyValueForMap() = lazy {
    annotations.associateBy { it.classId?.asSingleFqName() }
}

interface JavaPackage : JavaElement, JavaAnnotationOwner {
    val fqName: FqName
    val subPackages: Collection<JavaPackage>

    fun getClasses(nameFilter: (Name) -> Boolean): Collection<JavaClass>
}

interface JavaClassifier : JavaNamedElement, JavaAnnotationOwner

interface JavaClass : JavaClassifier, JavaTypeParameterListOwner, JavaModifierListOwner {
    val fqName: FqName?

    val supertypes: Collection<JavaClassifierType>
    val innerClassNames: Collection<Name>
    fun findInnerClass(name: Name): JavaClass?
    val outerClass: JavaClass?

    val isInterface: Boolean
    val isAnnotationType: Boolean
    val isEnum: Boolean
    val isRecord: Boolean
    val isSealed: Boolean
    val permittedTypes: Sequence<JavaClassifierType>
    val lightClassOriginKind: LightClassOriginKind?

    /** Returns the original ClsJavaClass in the case of decompiled light classes */
    val originalClsJavaClass: JavaClass get() = this

    val methods: Collection<JavaMethod>
    val fields: Collection<JavaField>
    val constructors: Collection<JavaConstructor>
    val recordComponents: Collection<JavaRecordComponent>
    fun hasDefaultConstructor(): Boolean
}

val JavaClass.classId: ClassId?
    get() = outerClass?.classId?.createNestedClassId(name) ?: fqName?.let(ClassId::topLevel)

enum class LightClassOriginKind {
    SOURCE, BINARY
}

interface JavaMember : JavaModifierListOwner, JavaAnnotationOwner, JavaNamedElement {
    val containingClass: JavaClass
}

interface JavaMethod : JavaMember, JavaTypeParameterListOwner {
    val valueParameters: List<JavaValueParameter>
    val returnType: JavaType

    // WARNING: computing the default value may lead to an exception in the compiler because of IDEA-207252.
    // If you only need to check default value presence, use `hasAnnotationParameterDefaultValue` instead.
    val annotationParameterDefaultValue: JavaAnnotationArgument?

    val hasAnnotationParameterDefaultValue: Boolean
        get() = annotationParameterDefaultValue != null

    val isNative: Boolean
}

interface JavaField : JavaMember {
    val isEnumEntry: Boolean
    val type: JavaType
    val initializerValue: Any?
    val hasConstantNotNullInitializer: Boolean
}

interface JavaConstructor : JavaMember, JavaTypeParameterListOwner {
    val valueParameters: List<JavaValueParameter>
}

interface JavaValueParameter : JavaAnnotationOwner {
    val name: Name?
    val type: JavaType
    val isVararg: Boolean
    val isFromSource: Boolean
}

interface JavaRecordComponent : JavaMember {
    val type: JavaType
    val isVararg: Boolean
}

interface JavaTypeParameter : JavaClassifier {
    val upperBounds: Collection<JavaClassifierType>
}
