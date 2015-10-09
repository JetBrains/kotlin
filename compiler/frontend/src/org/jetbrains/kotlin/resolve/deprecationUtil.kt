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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.annotations.argumentValue

private val JAVA_DEPRECATED = FqName("java.lang.Deprecated")

fun DeclarationDescriptor.getDeprecatedAnnotation(): Pair<AnnotationDescriptor, DeclarationDescriptor>? {
    val ownAnnotation = getDeclaredDeprecatedAnnotation(AnnotationUseSiteTarget.getAssociatedUseSiteTarget(this))
    if (ownAnnotation != null)
        return ownAnnotation to this

    when (this) {
        is ConstructorDescriptor -> {
            val classDescriptor = getContainingDeclaration()
            val classAnnotation = classDescriptor.getDeclaredDeprecatedAnnotation()
            if (classAnnotation != null)
                return classAnnotation to classDescriptor
        }
        is PropertyAccessorDescriptor -> {
            val propertyDescriptor = correspondingProperty

            val target = if (this is PropertyGetterDescriptor) AnnotationUseSiteTarget.PROPERTY_GETTER else AnnotationUseSiteTarget.PROPERTY_SETTER
            val accessorAnnotation = propertyDescriptor.getDeclaredDeprecatedAnnotation(target, false)
            if (accessorAnnotation != null)
                return accessorAnnotation to this

            val classDescriptor = containingDeclaration as? ClassDescriptor
            if (classDescriptor != null && classDescriptor.isCompanionObject) {
                val classAnnotation = classDescriptor.getDeclaredDeprecatedAnnotation()
                if (classAnnotation != null)
                    return classAnnotation to classDescriptor
            }
        }
    }
    return null
}

private fun DeclarationDescriptor.getDeclaredDeprecatedAnnotation(
        target: AnnotationUseSiteTarget? = null,
        findAnnotationsWithoutTarget: Boolean = true
): AnnotationDescriptor? {
    if (findAnnotationsWithoutTarget) {
        val annotations = annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.deprecated) ?: annotations.findAnnotation(JAVA_DEPRECATED)
        if (annotations != null) return annotations
    }

    if (target != null) {
        return Annotations.findUseSiteTargetedAnnotation(annotations, target, KotlinBuiltIns.FQ_NAMES.deprecated)
               ?: Annotations.findUseSiteTargetedAnnotation(annotations, target, JAVA_DEPRECATED)
    }

    return null
}

// Reflects values from kotlin.DeprecationLevel
enum class DeprecationLevelValue {
    WARNING, ERROR, HIDDEN
}

fun AnnotationDescriptor.getDeprecatedAnnotationLevel(): DeprecationLevelValue? {
    val level = this.argumentValue("level") as? ClassDescriptor

    return when(level?.name?.asString()) {
        "WARNING" -> DeprecationLevelValue.WARNING
        "ERROR" -> DeprecationLevelValue.ERROR
        "HIDDEN" -> DeprecationLevelValue.HIDDEN
        else -> null
    }
}

fun DeclarationDescriptor.getDeprecatedAnnotationLevel(): DeprecationLevelValue? {
    return getDeprecatedAnnotation()?.first?.getDeprecatedAnnotationLevel()
}

@Deprecated("Should be removed together with kotlin.HiddenDeclaration")
private val HIDDEN_ANNOTATION_FQ_NAME = FqName("kotlin.HiddenDeclaration")

fun DeclarationDescriptor.isAnnotatedAsHidden(): Boolean {
    return annotations.findAnnotation(HIDDEN_ANNOTATION_FQ_NAME) != null
        || getDeprecatedAnnotationLevel() == DeprecationLevelValue.HIDDEN
}
