/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

    /**
     * Whether the annotation class reference is already resolved to a fully qualified name.
     * Returns true for PSI-based implementations (where PSI resolves names).
     * Returns false for java-direct when the annotation name is unqualified and not imported.
     */
    val isResolved: Boolean
        get() = true

    /**
     * Resolves the annotation class using the provided callback.
     * Used by java-direct to resolve unqualified annotation names via java.lang and star imports.
     *
     * @param tryResolve callback that returns true if the given ClassId exists
     * @return the resolved ClassId, or null if not resolved
     */
    fun resolveAnnotation(tryResolve: (ClassId) -> Boolean): ClassId? = classId
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

    /**
     * Whether [resolveInitializerValue] does anything beyond returning [initializerValue]. Lets
     * the caller skip allocating the callback closure when it would be ignored. PSI/binary fields
     * already evaluate Java-side constant expressions at structure-build time and cannot have a
     * non-Java reference here, so they inherit `false`. java-direct overrides this to `true` for
     * fields whose initializer is a non-literal reference that may need cross-language resolution.
     */
    val supportsExternalInitializerResolution: Boolean get() = false

    /**
     * Resolves the initializer value using a callback that can resolve external references.
     * This is used for cross-language constant evaluation where Java fields reference Kotlin constants.
     *
     * @param resolveReference callback that resolves a qualified reference (e.g., "OtherClass.FIELD")
     *        to its constant value. Returns null if the reference cannot be resolved.
     * @return the evaluated constant value, or null if evaluation fails
     */
    fun resolveInitializerValue(resolveReference: (classQualifier: String?, fieldName: String) -> Any?): Any? {
        return initializerValue
    }
}

interface JavaConstructor : JavaMember, JavaTypeParameterListOwner {
    val valueParameters: List<JavaValueParameter>
}

interface JavaValueParameter : JavaAnnotationOwner {
    /**
     * The name of the parameter if present and not auto-generated
     *
     * @see nameOrGeneratedName
     */
    val name: Name?
    val type: JavaType
    val isVararg: Boolean
    val isFromSource: Boolean

    /**
     * The name of the parameter if present or the generated name (like 'p0')
     * if the parameter is the binary one and its name is auto-generated.
     *
     * Should be preferred over [name] where possible since it is expected to be more performant
     */
    val nameOrGeneratedName: Name? get() = name
}

interface JavaRecordComponent : JavaMember {
    val type: JavaType
    val isVararg: Boolean
}

interface JavaTypeParameter : JavaClassifier {
    val upperBounds: Collection<JavaClassifierType>
}
