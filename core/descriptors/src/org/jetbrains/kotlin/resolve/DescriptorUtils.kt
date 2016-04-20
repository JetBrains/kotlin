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

package org.jetbrains.kotlin.resolve.descriptorUtil

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.check

fun ClassDescriptor.getClassObjectReferenceTarget(): ClassDescriptor = companionObjectDescriptor ?: this

fun DeclarationDescriptor.getImportableDescriptor(): DeclarationDescriptor {
    return when {
        this is ConstructorDescriptor -> containingDeclaration
        this is PropertyAccessorDescriptor -> correspondingProperty
        else -> this
    }
}

val DeclarationDescriptor.fqNameUnsafe: FqNameUnsafe
    get() = DescriptorUtils.getFqName(this)

val DeclarationDescriptor.fqNameSafe: FqName
    get() = DescriptorUtils.getFqNameSafe(this)

val DeclarationDescriptor.isExtension: Boolean
    get() = this is CallableDescriptor && extensionReceiverParameter != null

val DeclarationDescriptor.module: ModuleDescriptor
    get() = DescriptorUtils.getContainingModule(this)

fun ModuleDescriptor.resolveTopLevelClass(topLevelClassFqName: FqName, location: LookupLocation): ClassDescriptor? {
    assert(!topLevelClassFqName.isRoot)
    return getPackage(topLevelClassFqName.parent()).memberScope.getContributedClassifier(topLevelClassFqName.shortName(), location) as? ClassDescriptor
}

val ClassDescriptor.classId: ClassId
    get() {
        val owner = containingDeclaration
        if (owner is PackageFragmentDescriptor) {
            return ClassId(owner.fqName, name)
        }
        else if (owner is ClassDescriptor) {
            return owner.classId.createNestedClassId(name)
        }
        throw IllegalStateException("Illegal container: $owner")
    }

val ClassDescriptor.hasCompanionObject: Boolean get() = companionObjectDescriptor != null

val ClassDescriptor.hasClassValueDescriptor: Boolean get() = classValueDescriptor != null

val ClassDescriptor.classValueDescriptor: ClassDescriptor?
    get() = if (kind.isSingleton) this else companionObjectDescriptor

val ClassDescriptor.classValueTypeDescriptor: ClassDescriptor?
    get() = when (kind) {
        OBJECT -> this
        ENUM_ENTRY -> {
            // enum entry has the type of enum class
            val container = this.containingDeclaration
            assert(container is ClassDescriptor && container.kind == ENUM_CLASS)
            container as ClassDescriptor
        }
        else -> companionObjectDescriptor
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
        var parent = containingDeclaration as? ClassDescriptor
        return parent != null && Visibilities.isPrivate(parent.visibility)
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

val ClassDescriptor.secondaryConstructors: List<ConstructorDescriptor>
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

/**
 * @return `true` iff the parameter has a default value, i.e. declares it or inherits by overriding a parameter which has a default value.
 */
fun ValueParameterDescriptor.hasDefaultValue(): Boolean {
    return DFS.ifAny(
            listOf(this),
            DFS.Neighbors<ValueParameterDescriptor> { current ->
                current.overriddenDescriptors.map(ValueParameterDescriptor::getOriginal)
            },
            ValueParameterDescriptor::declaresDefaultValue
    )
}

fun Annotated.isRepeatableAnnotation(): Boolean =
        annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.repeatable) != null

fun Annotated.isDocumentedAnnotation(): Boolean =
        annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.mustBeDocumented) != null

fun Annotated.getAnnotationRetention(): KotlinRetention? {
    val annotationEntryDescriptor = annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.retention) ?: return null
    val retentionArgumentValue = annotationEntryDescriptor.allValueArguments.entries.firstOrNull {
        it.key.name.asString() == "value"
    }?.value as? EnumValue ?: return null
    return KotlinRetention.valueOf(retentionArgumentValue.value.name.asString())
}

val DeclarationDescriptor.parentsWithSelf: Sequence<DeclarationDescriptor>
    get() = generateSequence(this, { it.containingDeclaration })

val DeclarationDescriptor.parents: Sequence<DeclarationDescriptor>
    get() = parentsWithSelf.drop(1)

val CallableMemberDescriptor.propertyIfAccessor: CallableMemberDescriptor
    get() = if (this is PropertyAccessorDescriptor) correspondingProperty else this

fun CallableDescriptor.fqNameOrNull(): FqName? = fqNameUnsafe.check { it.isSafe }?.toSafe()

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

fun CallableDescriptor.overriddenTreeUniqueAsSequence(useOriginal: Boolean): Sequence<CallableDescriptor> {
    val set = hashSetOf<CallableDescriptor>()

    fun CallableDescriptor.doBuildOverriddenTreeAsSequence(): Sequence<CallableDescriptor> {
        return with(if (useOriginal) original else this) {
            if (original in set)
                emptySequence()
            else {
                set += original
                sequenceOf(this) + overriddenDescriptors.asSequence().flatMap { it.doBuildOverriddenTreeAsSequence() }
            }
        }
    }

    return doBuildOverriddenTreeAsSequence()
}
