/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.descriptorUtil

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.ENUM_VALUE_OF
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.impl.DescriptorDerivedFromTypeAlias
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.getContainingClass
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.checker.REFINER_CAPABILITY
import org.jetbrains.kotlin.types.checker.TypeRefinementSupport
import org.jetbrains.kotlin.types.TypeRefinement
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.SmartList

private val RETENTION_PARAMETER_NAME = Name.identifier("value")

fun ClassDescriptor.getClassObjectReferenceTarget(): ClassDescriptor = companionObjectDescriptor ?: this

fun DeclarationDescriptor.getImportableDescriptor(): DeclarationDescriptor =
    when (this) {
        is DescriptorDerivedFromTypeAlias -> typeAliasDescriptor
        is ConstructorDescriptor -> containingDeclaration
        is PropertyAccessorDescriptor -> correspondingProperty
        else -> this
    }

val DeclarationDescriptor.fqNameUnsafe: FqNameUnsafe
    get() = DescriptorUtils.getFqName(this)

val DeclarationDescriptor.fqNameSafe: FqName
    get() = DescriptorUtils.getFqNameSafe(this)

val DeclarationDescriptor.isExtension: Boolean
    get() = this is CallableDescriptor && extensionReceiverParameter != null

val DeclarationDescriptor.module: ModuleDescriptor
    get() = DescriptorUtils.getContainingModule(this)

val DeclarationDescriptor.platform: TargetPlatform?
    get() = module.platform

fun ModuleDescriptor.resolveTopLevelClass(topLevelClassFqName: FqName, location: LookupLocation): ClassDescriptor? {
    assert(!topLevelClassFqName.isRoot)
    return getPackage(topLevelClassFqName.parent()).memberScope.getContributedClassifier(
        topLevelClassFqName.shortName(),
        location
    ) as? ClassDescriptor
}

val ClassifierDescriptorWithTypeParameters.denotedClassDescriptor: ClassDescriptor?
    get() = when (this) {
        is ClassDescriptor -> this
        is TypeAliasDescriptor -> classDescriptor
        else -> throw UnsupportedOperationException("Unexpected descriptor kind: $this")
    }

// Used in https://plugins.jetbrains.com/plugin/10346-extsee
@Deprecated("The one below with receiver type ClassifierDescriptor? should be used", level = DeprecationLevel.HIDDEN)
val ClassifierDescriptorWithTypeParameters.classId: ClassId?
    get() = (this as ClassifierDescriptor?).classId

val ClassifierDescriptor?.classId: ClassId?
    get() = this?.containingDeclaration?.let { owner ->
        when (owner) {
            is PackageFragmentDescriptor -> ClassId(owner.fqName, name)
            is ClassifierDescriptorWithTypeParameters -> owner.classId?.createNestedClassId(name)
            else -> null
        }
    }

val ClassifierDescriptorWithTypeParameters.hasCompanionObject: Boolean
    get() = denotedClassDescriptor?.companionObjectDescriptor != null

val ClassDescriptor.hasClassValueDescriptor: Boolean get() = classValueDescriptor != null

val ClassDescriptor.classValueDescriptor: ClassDescriptor?
    get() =
        if (kind.isSingleton)
            this
        else
            companionObjectDescriptor

val ClassifierDescriptorWithTypeParameters.classValueTypeDescriptor: ClassDescriptor?
    get() = denotedClassDescriptor?.let {
        when (it.kind) {
            OBJECT -> it
            ENUM_ENTRY -> {
                // enum entry has the type of enum class
                val container = this.containingDeclaration
                assert(container is ClassDescriptor && container.kind == ENUM_CLASS)
                container as ClassDescriptor
            }
            else -> it.companionObjectDescriptor
        }
    }

/** If a literal of this class can be used as a value, returns the type of this value */
val ClassDescriptor.classValueType: KotlinType?
    get() = classValueTypeDescriptor?.defaultType

val DeclarationDescriptorWithVisibility.isEffectivelyPublicApi: Boolean
    get() = effectiveVisibility().publicApi

val DeclarationDescriptorWithVisibility.isEffectivelyPrivateApi: Boolean
    get() = effectiveVisibility().privateApi


val DeclarationDescriptor.isInsidePrivateClass: Boolean
    get() {
        val parent = containingDeclaration as? ClassDescriptor
        return parent != null && DescriptorVisibilities.isPrivate(parent.visibility)
    }


fun ClassDescriptor.getSuperClassNotAny(): ClassDescriptor? {
    for (supertype in defaultType.constructor.supertypes) {
        if (!KotlinBuiltIns.isAnyOrNullableAny(supertype)) {
            val superClassifier = supertype.constructor.declarationDescriptor
            if (DescriptorUtils.isClassOrEnumClass(superClassifier)) {
                return superClassifier as ClassDescriptor
            }
        }
    }
    return null
}

fun ClassDescriptor.getSuperClassOrAny(): ClassDescriptor = getSuperClassNotAny() ?: builtIns.any

fun ClassDescriptor.getSuperInterfaces(): List<ClassDescriptor> =
    defaultType.constructor.supertypes
        .filterNot { KotlinBuiltIns.isAnyOrNullableAny(it) }
        .mapNotNull {
            val superClassifier = it.constructor.declarationDescriptor
            if (DescriptorUtils.isInterface(superClassifier)) superClassifier as ClassDescriptor
            else null
        }

val ClassDescriptor.secondaryConstructors: List<ClassConstructorDescriptor>
    get() = constructors.filterNot { it.isPrimary }

val DeclarationDescriptor.builtIns: KotlinBuiltIns
    get() = module.builtIns

/**
 * Returns containing declaration of dispatch receiver for callable adjusted to fake-overridden cases
 *
 * open class A {
 *   fun foo() = 1
 * }
 * class B : A()
 *
 * for A.foo -> returns A (dispatch receiver parameter is A)
 * for B.foo -> returns B (dispatch receiver parameter is still A, but it's fake-overridden in B, so it's containing declaration is B)
 *
 * class Outer {
 *   inner class Inner()
 * }
 *
 * for constructor of Outer.Inner -> returns Outer (dispatch receiver parameter is Outer, but it's containing declaration is Inner)
 *
 */
fun CallableDescriptor.getOwnerForEffectiveDispatchReceiverParameter(): DeclarationDescriptor? {
    if (this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        return getContainingDeclaration()
    }
    return dispatchReceiverParameter?.containingDeclaration
}

fun ValueParameterDescriptor.declaresOrInheritsDefaultValue(): Boolean {
    return DFS.ifAny(
        listOf(this),
        { current -> current.overriddenDescriptors.map(ValueParameterDescriptor::getOriginal) },
        ValueParameterDescriptor::declaresDefaultValue
    )
}

// Note that on JVM, an annotation class is also considered repeatable if it's annotated with java.lang.annotation.Repeatable.
// See JvmPlatformAnnotationFeaturesSupport.
fun Annotated.isAnnotatedWithKotlinRepeatable(): Boolean =
    annotations.findAnnotation(StandardNames.FqNames.repeatable) != null

fun Annotated.isDocumentedAnnotation(): Boolean =
    annotations.findAnnotation(StandardNames.FqNames.mustBeDocumented) != null

fun Annotated.getAnnotationRetention(): KotlinRetention? {
    return annotations.findAnnotation(StandardNames.FqNames.retention)?.getAnnotationRetention()
}

fun AnnotationDescriptor.getAnnotationRetention(): KotlinRetention? {
    val retentionArgumentValue = allValueArguments[RETENTION_PARAMETER_NAME] as? EnumValue ?: return null

    val retentionArgumentValueName = retentionArgumentValue.enumEntryName.asString()
    return KotlinRetention.values().firstOrNull { it.name == retentionArgumentValueName }
}

val Annotated.nonSourceAnnotations: List<AnnotationDescriptor>
    get() = annotations.filterOutSourceAnnotations()

fun Iterable<AnnotationDescriptor>.filterOutSourceAnnotations(): List<AnnotationDescriptor> =
    filterNot(AnnotationDescriptor::isSourceAnnotation)

val AnnotationDescriptor.isSourceAnnotation: Boolean
    get() {
        val classDescriptor = annotationClass
        return classDescriptor == null || classDescriptor.getAnnotationRetention() == KotlinRetention.SOURCE
    }

val DeclarationDescriptor.parentsWithSelf: Sequence<DeclarationDescriptor>
    get() = generateSequence(this, { it.containingDeclaration })

val DeclarationDescriptor.parents: Sequence<DeclarationDescriptor>
    get() = parentsWithSelf.drop(1)

val CallableMemberDescriptor.propertyIfAccessor: CallableMemberDescriptor
    get() = if (this is PropertyAccessorDescriptor) correspondingProperty else this

fun DeclarationDescriptor.fqNameOrNull(): FqName? = fqNameUnsafe.takeIf { it.isSafe }?.toSafe()

fun CallableMemberDescriptor.firstOverridden(
    useOriginal: Boolean = false,
    predicate: (CallableMemberDescriptor) -> Boolean
): CallableMemberDescriptor? {
    var result: CallableMemberDescriptor? = null
    return DFS.dfs(listOf(this),
                   { current ->
                       val descriptor = if (useOriginal) current?.original else current
                       descriptor?.overriddenDescriptors ?: emptyList()
                   },
                   object : DFS.AbstractNodeHandler<CallableMemberDescriptor, CallableMemberDescriptor?>() {
                       override fun beforeChildren(current: CallableMemberDescriptor) = result == null
                       override fun afterChildren(current: CallableMemberDescriptor) {
                           if (result == null && predicate(current)) {
                               result = current
                           }
                       }

                       override fun result(): CallableMemberDescriptor? = result
                   }
    )
}

fun CallableMemberDescriptor.setSingleOverridden(overridden: CallableMemberDescriptor) {
    overriddenDescriptors = listOf(overridden)
}

fun CallableMemberDescriptor.overriddenTreeAsSequence(useOriginal: Boolean): Sequence<CallableMemberDescriptor> =
    with(if (useOriginal) original else this) {
        sequenceOf(this) + overriddenDescriptors.asSequence().flatMap { it.overriddenTreeAsSequence(useOriginal) }
    }

fun <D : CallableDescriptor> D.overriddenTreeUniqueAsSequence(useOriginal: Boolean): Sequence<D> {
    val set = hashSetOf<D>()

    @Suppress("UNCHECKED_CAST")
    fun D.doBuildOverriddenTreeAsSequence(): Sequence<D> {
        return with(if (useOriginal) original as D else this) {
            if (original in set)
                emptySequence()
            else {
                set += original as D
                sequenceOf(this) + (overriddenDescriptors as Collection<D>).asSequence().flatMap { it.doBuildOverriddenTreeAsSequence() }
            }
        }
    }

    return doBuildOverriddenTreeAsSequence()
}

fun CallableDescriptor.varargParameterPosition() =
    valueParameters.indexOfFirst { it.varargElementType != null }

/**
 * When `Inner` is used as type outside of `Outer` class all type arguments should be specified, e.g. `Outer<String, Int>.Inner<Double>`
 * However, it's not necessary inside Outer's members, only the last one should be specified there.
 * So this function return a list of arguments that should be used if relevant arguments weren't specified explicitly inside the [scopeOwner].
 *
 * Examples:
 * for `Outer` class the map will contain: Outer -> (X, Y) (i.e. defaultType mapping)
 * for `Derived` class the map will contain: Derived -> (E), Outer -> (E, String)
 * for `A.B` class the map will contain: B -> (), Outer -> (Int, CharSequence), A -> ()
 *
 * open class Outer<X, Y> {
 *  inner class Inner<Z>
 * }
 *
 * class Derived<E> : Outer<E, String>()
 *
 * class A : Outer<String, Double>() {
 *   inner class B : Outer<Int, CharSequence>()
 * }
 */
fun findImplicitOuterClassArguments(scopeOwner: ClassDescriptor, outerClass: ClassDescriptor): List<TypeProjection>? {
    for (current in scopeOwner.classesFromInnerToOuter()) {
        for (supertype in current.getAllSuperClassesTypesIncludeItself()) {
            val classDescriptor = supertype.constructor.declarationDescriptor as ClassDescriptor
            if (classDescriptor == outerClass) return supertype.arguments
        }
    }

    return null
}

private fun ClassDescriptor.classesFromInnerToOuter() = generateSequence(this) {
    if (it.isInner)
        it.containingDeclaration.original as? ClassDescriptor
    else
        null
}

private fun ClassDescriptor.getAllSuperClassesTypesIncludeItself(): List<KotlinType> {
    val result = arrayListOf<KotlinType>()
    var current: KotlinType = defaultType

    while (!current.isAnyOrNullableAny()) {
        result.add(current)
        val next = DescriptorUtils.getSuperClassType(current.constructor.declarationDescriptor as ClassDescriptor)
        current = TypeSubstitutor.create(current).substitute(next, Variance.INVARIANT) ?: break
    }

    return result
}

fun FunctionDescriptor.isEnumValueOfMethod(): Boolean {
    val methodTypeParameters = valueParameters
    val nullableString = builtIns.stringType.makeNullable()
    return ENUM_VALUE_OF == name
            && methodTypeParameters.size == 1
            && KotlinTypeChecker.DEFAULT.isSubtypeOf(methodTypeParameters[0].type, nullableString)
}

val DeclarationDescriptor.isExtensionProperty: Boolean
    get() = this is PropertyDescriptor && extensionReceiverParameter != null

fun ClassDescriptor.getAllSuperclassesWithoutAny() =
    generateSequence(getSuperClassNotAny(), ClassDescriptor::getSuperClassNotAny).toCollection(SmartList<ClassDescriptor>())

fun ClassifierDescriptor.getAllSuperClassifiers(): Sequence<ClassifierDescriptor> {
    val set = hashSetOf<ClassifierDescriptor>()

    fun ClassifierDescriptor.doGetAllSuperClassesAndInterfaces(): Sequence<ClassifierDescriptor> =
        if (original in set) {
            emptySequence()
        } else {
            set += original
            sequenceOf(original) + typeConstructor.supertypes.asSequence().flatMap {
                it.constructor.declarationDescriptor?.doGetAllSuperClassesAndInterfaces() ?: sequenceOf()
            }
        }

    return doGetAllSuperClassesAndInterfaces()
}

fun DeclarationDescriptor.isPublishedApi(): Boolean {
    val descriptor = if (this is CallableMemberDescriptor) DescriptorUtils.getDirectMember(this) else this
    return descriptor.annotations.hasAnnotation(StandardNames.FqNames.publishedApi)
}

fun DeclarationDescriptor.isAncestorOf(descriptor: DeclarationDescriptor, strict: Boolean): Boolean =
    DescriptorUtils.isAncestor(this, descriptor, strict)

fun DeclarationDescriptor.isCompanionObject(): Boolean = DescriptorUtils.isCompanionObject(this)

fun ClassDescriptor.isSubclassOf(superclass: ClassDescriptor): Boolean = DescriptorUtils.isSubclass(this, superclass)

val AnnotationDescriptor.annotationClass: ClassDescriptor?
    get() = type.constructor.declarationDescriptor as? ClassDescriptor

fun AnnotationDescriptor.firstArgument(): ConstantValue<*>? = allValueArguments.values.firstOrNull()

fun MemberDescriptor.isEffectivelyExternal(): Boolean {
    if (isExternal) return true

    if (this is PropertyAccessorDescriptor) {
        val variableDescriptor = correspondingProperty
        if (variableDescriptor.isEffectivelyExternal()) return true
    }

    if (this is PropertyDescriptor) {
        if (getter?.isExternal == true &&
            (!isVar || setter?.isExternal == true)
        ) return true
    }

    val containingClass = getContainingClass(this)
    return containingClass != null && containingClass.isEffectivelyExternal()
}

fun isParameterOfAnnotation(parameterDescriptor: ParameterDescriptor): Boolean =
    parameterDescriptor.containingDeclaration.isAnnotationConstructor()

fun DeclarationDescriptor.isAnnotationConstructor(): Boolean =
    this is ConstructorDescriptor && DescriptorUtils.isAnnotationClass(this.constructedClass)

fun DeclarationDescriptor.isPrimaryConstructorOfInlineClass(): Boolean =
    this is ConstructorDescriptor && this.isPrimary && this.constructedClass.isInlineClass()

@TypeRefinement
fun ModuleDescriptor.getKotlinTypeRefiner(): KotlinTypeRefiner =
    when (val refinerCapability = getCapability(REFINER_CAPABILITY)?.value) {
        is TypeRefinementSupport.Enabled -> refinerCapability.typeRefiner
        else -> KotlinTypeRefiner.Default
    }

@OptIn(TypeRefinement::class)
fun ModuleDescriptor.isTypeRefinementEnabled(): Boolean =
    getCapability(REFINER_CAPABILITY)?.value?.isEnabled == true

val VariableDescriptor.isUnderscoreNamed
    get() = !name.isSpecial && name.identifier == "_"

private fun <D : CallableDescriptor> D.containsStubTypes() =
    valueParameters.any { parameter -> parameter.type.contains { it is StubTypeForBuilderInference } }
            || returnType?.contains { it is StubTypeForBuilderInference } == true
            || dispatchReceiverParameter?.type?.contains { it is StubTypeForBuilderInference } == true
            || extensionReceiverParameter?.type?.contains { it is StubTypeForBuilderInference } == true

fun <D : CallableDescriptor> D.shouldBeSubstituteWithStubTypes() =
    valueParameters.none { it.type.isError }
            && returnType?.isError != true
            && dispatchReceiverParameter?.type?.isError != true
            && extensionReceiverParameter?.type?.isError != true
            && containsStubTypes()
