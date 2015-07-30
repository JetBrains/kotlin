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
import org.jetbrains.kotlin.descriptors.ClassKind.ENUM_CLASS
import org.jetbrains.kotlin.descriptors.ClassKind.ENUM_ENTRY
import org.jetbrains.kotlin.descriptors.ClassKind.OBJECT
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.types.JetType

public fun ClassDescriptor.getClassObjectReferenceTarget(): ClassDescriptor = getCompanionObjectDescriptor() ?: this

public fun DeclarationDescriptor.getImportableDescriptor(): DeclarationDescriptor {
    return when {
        this is ConstructorDescriptor -> getContainingDeclaration()
        this is PropertyAccessorDescriptor -> getCorrespondingProperty()
        else -> this
    }
}

public val DeclarationDescriptor.fqNameUnsafe: FqNameUnsafe
    get() = DescriptorUtils.getFqName(this)

public val DeclarationDescriptor.fqNameSafe: FqName
    get() = DescriptorUtils.getFqNameSafe(this)

public val DeclarationDescriptor.isExtension: Boolean
    get() = this is CallableDescriptor && getExtensionReceiverParameter() != null

public val DeclarationDescriptor.module: ModuleDescriptor
    get() = DescriptorUtils.getContainingModule(this)

public fun ModuleDescriptor.resolveTopLevelClass(topLevelClassFqName: FqName): ClassDescriptor? {
    assert(!topLevelClassFqName.isRoot())
    return getPackage(topLevelClassFqName.parent()).memberScope.getClassifier(topLevelClassFqName.shortName()) as? ClassDescriptor
}

public val ClassDescriptor.classId: ClassId
    get() {
        val owner = getContainingDeclaration()
        if (owner is PackageFragmentDescriptor) {
            return ClassId(owner.fqName, getName())
        }
        else if (owner is ClassDescriptor) {
            return owner.classId.createNestedClassId(getName())
        }
        throw IllegalStateException("Illegal container: $owner")
    }

public val ClassDescriptor.hasClassObjectType: Boolean get() = classObjectType != null

/** If a literal of this class can be used as a value, returns the type of this value */
public val ClassDescriptor.classObjectType: JetType?
    get() {
        val correspondingDescriptor = when (this.getKind()) {
            OBJECT -> this
            // enum entry has the type of enum class
            ENUM_ENTRY -> {
                val container = this.getContainingDeclaration()
                assert(container is ClassDescriptor && container.getKind() == ENUM_CLASS)
                container as ClassDescriptor
            }
            else -> getCompanionObjectDescriptor()
        }
        return correspondingDescriptor?.getDefaultType()
    }

public val DeclarationDescriptorWithVisibility.isEffectivelyPublicApi: Boolean
    get() {
        var parent: DeclarationDescriptorWithVisibility? = this

        while (parent != null) {
            if (!parent.getVisibility().isPublicAPI()) return false

            parent = DescriptorUtils.getParentOfType(parent, javaClass<DeclarationDescriptorWithVisibility>())
        }

        return true
    }

public fun ClassDescriptor.getSuperClassNotAny(): ClassDescriptor? {
    for (supertype in getDefaultType().getConstructor().getSupertypes()) {
        val superClassifier = supertype.getConstructor().getDeclarationDescriptor()
        if (!KotlinBuiltIns.isAnyOrNullableAny(supertype) &&
            (DescriptorUtils.isClass(superClassifier) || DescriptorUtils.isEnumClass(superClassifier))) {
            return superClassifier as ClassDescriptor
        }
    }
    return null
}

public fun ClassDescriptor.getSuperClassOrAny(): ClassDescriptor = getSuperClassNotAny() ?: builtIns.getAny()

public val ClassDescriptor.secondaryConstructors: List<ConstructorDescriptor>
    get() = getConstructors().filterNot { it.isPrimary() }

public val DeclarationDescriptor.builtIns: KotlinBuiltIns
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
public fun CallableDescriptor.getOwnerForEffectiveDispatchReceiverParameter(): DeclarationDescriptor? {
    if (this is CallableMemberDescriptor && getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        return getContainingDeclaration()
    }
    return getDispatchReceiverParameter()?.getContainingDeclaration()
}

private fun Annotated.isAnnotationPropertyTrue(name: String, defaultValue: Boolean = false): Boolean {
    val annotationEntryDescriptor = annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.annotation) ?: return defaultValue
    val repeatableArgumentValue = annotationEntryDescriptor.allValueArguments.entrySet().firstOrNull {
        name == it.key.name.asString()
    }?.getValue() as? BooleanValue ?: return defaultValue
    return repeatableArgumentValue.value
}

public fun Annotated.isRepeatableAnnotation(): Boolean = isAnnotationPropertyTrue("repeatable")

public fun Annotated.isDocumentedAnnotation(): Boolean = isAnnotationPropertyTrue("mustBeDocumented")

public fun Annotated.getAnnotationRetention(): KotlinRetention? {
    val annotationEntryDescriptor = annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.annotation) ?: return null
    val retentionArgumentValue = annotationEntryDescriptor.allValueArguments.entrySet().firstOrNull {
        "retention" == it.key.name.asString()
    }?.getValue() as? EnumValue ?: return null
    return KotlinRetention.valueOf(retentionArgumentValue.value.name.asString())
}
