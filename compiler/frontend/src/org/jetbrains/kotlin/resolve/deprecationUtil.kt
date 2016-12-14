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

package org.jetbrains.kotlin.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DeprecationLevelValue.*
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import java.util.*

private val JAVA_DEPRECATED = FqName("java.lang.Deprecated")

interface Deprecation {
    val deprecationLevel: DeprecationLevelValue
    val message: String?
    val target: DeclarationDescriptor
}

fun Deprecation.deprecatedByOverriddenMessage(): String? = (this as? DeprecatedByOverridden)?.additionalMessage()

fun Deprecation.deprecatedByAnnotationReplaceWithExpression(): String? {
    val annotation = (this as? DeprecatedByAnnotation)?.annotation ?: return null
    val replaceWithAnnotation = annotation.argumentValue(kotlin.Deprecated::replaceWith.name)
                                        as? AnnotationDescriptor ?: return null

    return replaceWithAnnotation.argumentValue(kotlin.ReplaceWith::expression.name) as String
}

private data class DeprecatedByAnnotation(val annotation: AnnotationDescriptor, override val target: DeclarationDescriptor) : Deprecation {
    override val deprecationLevel: DeprecationLevelValue
        get() {
            val level = annotation.argumentValue("level") as? ClassDescriptor

            return when (level?.name?.asString()) {
                "WARNING" -> WARNING
                "ERROR" -> ERROR
                "HIDDEN" -> HIDDEN
                else -> WARNING
            }
        }

    override val message: String?
        get() = annotation.argumentValue("message") as? String
}

private data class DeprecatedByOverridden(private val deprecations: Collection<Deprecation>) : Deprecation {
    init {
        assert(deprecations.isNotEmpty())
        assert(deprecations.none {
            it is DeprecatedByOverridden
        })
    }

    override val deprecationLevel: DeprecationLevelValue = deprecations.map(Deprecation::deprecationLevel).min()!!

    override val target: DeclarationDescriptor
        get() = deprecations.first().target

    override val message: String
        get() {
            val message = deprecations.filter { it.deprecationLevel == this.deprecationLevel }.map { it.message }.toSet().joinToString(". ")
            return "${additionalMessage()}. $message"
        }

    internal fun additionalMessage() = "Overrides deprecated member in '${DescriptorUtils.getContainingClass(target)!!.fqNameSafe.asString()}'"
}

fun DeclarationDescriptor.getDeprecations(): List<Deprecation> {
    val deprecation = this.getDeprecationByAnnotation()
    if (deprecation != null) {
        return listOf(deprecation)
    }

    if (this is CallableMemberDescriptor) {
        return listOfNotNull(deprecationByOverridden(this))
    }

    return emptyList()
}

private fun deprecationByOverridden(root: CallableMemberDescriptor): Deprecation? {
    val visited = HashSet<CallableMemberDescriptor>()
    val deprecations = LinkedHashSet<Deprecation>()
    var hasUndeprecatedOverridden = false

    fun traverse(node: CallableMemberDescriptor) {
        if (node in visited) return

        visited.add(node)

        val deprecatedAnnotation = node.getDeprecationByAnnotation()
        val overriddenDescriptors = node.original.overriddenDescriptors
        when {
            deprecatedAnnotation != null -> {
                deprecations.add(deprecatedAnnotation)
            }
            overriddenDescriptors.isEmpty() -> {
                hasUndeprecatedOverridden = true
                return
            }
            else -> {
                overriddenDescriptors.forEach(::traverse)
            }
        }
    }

    traverse(root)

    if (hasUndeprecatedOverridden || deprecations.isEmpty()) return null

    return DeprecatedByOverridden(deprecations)
}

private fun DeclarationDescriptor.getDeprecationByAnnotation(): DeprecatedByAnnotation? {
    val ownAnnotation = getDeclaredDeprecatedAnnotation(AnnotationUseSiteTarget.getAssociatedUseSiteTarget(this))
    if (ownAnnotation != null)
        return DeprecatedByAnnotation(ownAnnotation, this)

    when (this) {
        is ConstructorDescriptor -> {
            val classDescriptor = containingDeclaration
            val classAnnotation = classDescriptor.getDeclaredDeprecatedAnnotation()
            if (classAnnotation != null)
                return DeprecatedByAnnotation(classAnnotation, classDescriptor)
            if (this is TypeAliasConstructorDescriptor) {
                val underlyingConstructorDeprecation = underlyingConstructorDescriptor.getDeprecationByAnnotation()
                if (underlyingConstructorDeprecation != null)
                    return underlyingConstructorDeprecation
            }
        }
        is PropertyAccessorDescriptor -> {
            val propertyDescriptor = correspondingProperty

            val target = if (this is PropertyGetterDescriptor) AnnotationUseSiteTarget.PROPERTY_GETTER else AnnotationUseSiteTarget.PROPERTY_SETTER
            val useSiteAnnotationOnProperty = propertyDescriptor.getDeclaredDeprecatedAnnotation(target, false)
            if (useSiteAnnotationOnProperty != null)
                return DeprecatedByAnnotation(useSiteAnnotationOnProperty, this)

            val propertyAnnotation = propertyDescriptor.getDeclaredDeprecatedAnnotation()
            if (propertyAnnotation != null)
                return DeprecatedByAnnotation(propertyAnnotation, propertyDescriptor)
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

internal fun createDeprecationDiagnostic(element: PsiElement, deprecation: Deprecation): Diagnostic {
    val targetOriginal = deprecation.target.original
    val diagnosticFactory = when (deprecation.deprecationLevel) {
        WARNING -> Errors.DEPRECATION
        ERROR -> Errors.DEPRECATION_ERROR
        HIDDEN -> Errors.DEPRECATION_ERROR
    }
    return diagnosticFactory.on(element, targetOriginal, deprecation.message ?: "")
}

// values from kotlin.DeprecationLevel
enum class DeprecationLevelValue {
    WARNING, ERROR, HIDDEN
}

fun DeclarationDescriptor.isDeprecatedHidden(): Boolean =
        getDeprecations().any { it.deprecationLevel == HIDDEN }

@JvmOverloads
fun DeclarationDescriptor.isHiddenInResolution(languageVersionSettings: LanguageVersionSettings, isSuperCall: Boolean = false): Boolean {
    if (this is FunctionDescriptor) {
        if (isHiddenToOvercomeSignatureClash) return true
        if (isHiddenForResolutionEverywhereBesideSupercalls && !isSuperCall) return true
    }

    if (!checkSinceKotlinVersionAccessibility(languageVersionSettings)) return true

    return isDeprecatedHidden()
}
