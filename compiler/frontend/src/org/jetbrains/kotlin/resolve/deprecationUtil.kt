/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.config.ApiVersion
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
import org.jetbrains.kotlin.resolve.calls.checkers.hasSubpackageOfKotlin
import org.jetbrains.kotlin.resolve.calls.checkers.isOperatorMod
import org.jetbrains.kotlin.resolve.calls.checkers.shouldWarnAboutDeprecatedModFromBuiltIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.SinceKotlinInfo
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.SmartList

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

private data class DeprecatedBySinceKotlinInfo(
        private val sinceKotlinInfo: SinceKotlinInfo,
        override val target: DeclarationDescriptor
) : Deprecation {
    override val deprecationLevel: DeprecationLevelValue
        get() = when (sinceKotlinInfo.level) {
            DeprecationLevel.WARNING -> WARNING
            DeprecationLevel.ERROR -> ERROR
            DeprecationLevel.HIDDEN -> HIDDEN
        }

    override val message: String?
        get() {
            val message = sinceKotlinInfo.message
            val errorCode = sinceKotlinInfo.errorCode
            if (message == null && errorCode == null) return null

            return buildString {
                if (message != null) {
                    append(message)
                    if (errorCode != null) {
                        append(" (error code $errorCode)")
                    }
                }
                else {
                    append("Error code $errorCode")
                }
            }
        }

    val sinceKotlinVersion: SinceKotlinInfo.Version
        get() = sinceKotlinInfo.version
}

fun DeclarationDescriptor.getDeprecations(languageVersionSettings: LanguageVersionSettings): List<Deprecation> {
    val deprecations = this.getOwnDeprecations(languageVersionSettings)
    if (deprecations.isNotEmpty()) {
        return deprecations
    }

    if (this is CallableMemberDescriptor) {
        return listOfNotNull(deprecationByOverridden(this, languageVersionSettings))
    }

    return emptyList()
}

private fun KotlinType.deprecationsByConstituentTypes(languageVersionSettings: LanguageVersionSettings): List<Deprecation> =
        SmartList<Deprecation>().also { deprecations ->
            TypeUtils.contains(this) { type ->
                type.constructor.declarationDescriptor?.run {
                    deprecations.addAll(getDeprecations(languageVersionSettings))
                }
                false
            }
        }

private fun deprecationByOverridden(root: CallableMemberDescriptor, languageVersionSettings: LanguageVersionSettings): Deprecation? {
    val visited = HashSet<CallableMemberDescriptor>()
    val deprecations = LinkedHashSet<Deprecation>()
    var hasUndeprecatedOverridden = false

    fun traverse(node: CallableMemberDescriptor) {
        if (node in visited) return

        visited.add(node)

        val deprecationsByAnnotation = node.getOwnDeprecations(languageVersionSettings)
        val overriddenDescriptors = node.original.overriddenDescriptors
        when {
            deprecationsByAnnotation.isNotEmpty() -> {
                deprecations.addAll(deprecationsByAnnotation)
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

private fun DeclarationDescriptor.getOwnDeprecations(languageVersionSettings: LanguageVersionSettings): List<Deprecation> {
    // The problem is that declaration `mod` in built-ins has @Deprecated annotation but actually it was deprecated only in version 1.1
    if (this is FunctionDescriptor && this.isOperatorMod() && this.hasSubpackageOfKotlin()) {
        if (!shouldWarnAboutDeprecatedModFromBuiltIns(languageVersionSettings)) {
            return emptyList()
        }
    }

    val result = SmartList<Deprecation>()

    fun addDeprecationIfPresent(target: DeclarationDescriptor) {
        val annotation = target.annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.deprecated)
                         ?: target.annotations.findAnnotation(JAVA_DEPRECATED)
        if (annotation != null) {
            result.add(DeprecatedByAnnotation(annotation, target))
        }

        val sinceKotlinInfo =
                (target as? DeserializedMemberDescriptor)?.sinceKotlinInfo
                ?: (target as? DeserializedClassDescriptor)?.sinceKotlinInfo
        if (sinceKotlinInfo != null) {
            // We're using ApiVersion because it's convenient to compare versions, "-api-version" is not involved in any way
            // TODO: usage of ApiVersion is confusing here, refactor
            if (ApiVersion.createBySinceKotlinInfo(sinceKotlinInfo) >
                ApiVersion.createByLanguageVersion(languageVersionSettings.languageVersion)) {
                result.add(DeprecatedBySinceKotlinInfo(sinceKotlinInfo, target))
            }
        }
    }

    fun addUseSiteTargetedDeprecationIfPresent(annotatedDescriptor: DeclarationDescriptor, useSiteTarget: AnnotationUseSiteTarget?) {
        if (useSiteTarget != null) {
            val annotation = Annotations.findUseSiteTargetedAnnotation(annotatedDescriptor.annotations, useSiteTarget, KotlinBuiltIns.FQ_NAMES.deprecated)
                             ?: Annotations.findUseSiteTargetedAnnotation(annotatedDescriptor.annotations, useSiteTarget, JAVA_DEPRECATED)
            if (annotation != null) {
                result.add(DeprecatedByAnnotation(annotation, this))
            }
        }
    }

    addDeprecationIfPresent(this)
    addUseSiteTargetedDeprecationIfPresent(this, AnnotationUseSiteTarget.getAssociatedUseSiteTarget(this))

    when (this) {
        is TypeAliasDescriptor -> {
            result.addAll(expandedType.deprecationsByConstituentTypes(languageVersionSettings))
        }
        is TypeAliasConstructorDescriptor -> {
            result.addAll(typeAliasDescriptor.getOwnDeprecations(languageVersionSettings))
        }
        is ConstructorDescriptor -> {
            addDeprecationIfPresent(containingDeclaration)
        }
        is PropertyAccessorDescriptor -> {
            addDeprecationIfPresent(correspondingProperty)

            addUseSiteTargetedDeprecationIfPresent(
                    correspondingProperty,
                    if (this is PropertyGetterDescriptor) AnnotationUseSiteTarget.PROPERTY_GETTER else AnnotationUseSiteTarget.PROPERTY_SETTER
            )
        }
    }

    return result.distinct()
}

internal fun createDeprecationDiagnostic(
        element: PsiElement, deprecation: Deprecation, languageVersionSettings: LanguageVersionSettings
): Diagnostic {
    val targetOriginal = deprecation.target.original
    if (deprecation is DeprecatedBySinceKotlinInfo) {
        val factory = when (deprecation.deprecationLevel) {
            WARNING -> Errors.SINCE_KOTLIN_INFO_DEPRECATION
            ERROR, HIDDEN -> Errors.SINCE_KOTLIN_INFO_DEPRECATION_ERROR
        }
        return factory.on(element, targetOriginal, deprecation.sinceKotlinVersion,
                          languageVersionSettings.languageVersion to deprecation.message)
    }

    val factory = when (deprecation.deprecationLevel) {
        WARNING -> Errors.DEPRECATION
        ERROR, HIDDEN -> Errors.DEPRECATION_ERROR
    }
    return factory.on(element, targetOriginal, deprecation.message ?: "")
}

// values from kotlin.DeprecationLevel
enum class DeprecationLevelValue {
    WARNING, ERROR, HIDDEN
}

fun DeclarationDescriptor.isDeprecatedHidden(languageVersionSettings: LanguageVersionSettings): Boolean =
        getDeprecations(languageVersionSettings).any { it.deprecationLevel == HIDDEN }

@JvmOverloads
fun DeclarationDescriptor.isHiddenInResolution(languageVersionSettings: LanguageVersionSettings, isSuperCall: Boolean = false): Boolean {
    if (this is FunctionDescriptor) {
        if (isHiddenToOvercomeSignatureClash) return true
        if (isHiddenForResolutionEverywhereBesideSupercalls && !isSuperCall) return true
    }

    if (!checkSinceKotlinVersionAccessibility(languageVersionSettings)) return true

    return isDeprecatedHidden(languageVersionSettings)
}
