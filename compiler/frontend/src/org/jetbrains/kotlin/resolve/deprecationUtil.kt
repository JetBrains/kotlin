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
import org.jetbrains.kotlin.descriptors.impl.DescriptorDerivedFromTypeAlias
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DeprecationLevelValue.*
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.calls.checkers.isOperatorMod
import org.jetbrains.kotlin.resolve.calls.checkers.shouldWarnAboutDeprecatedModFromBuiltIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.SinceKotlinInfo
import org.jetbrains.kotlin.storage.StorageManager
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

private data class DeprecatedTypealiasByAnnotation(
        val typeAliasTarget: TypeAliasDescriptor,
        val nested: DeprecatedByAnnotation
) : Deprecation {
    override val target get() = typeAliasTarget
    override val deprecationLevel get() = nested.deprecationLevel
    override val message get() = nested.message
}

private fun Deprecation.wrapInTypeAliasExpansion(typeAliasDescriptor: TypeAliasDescriptor) = when {
    this is DeprecatedByAnnotation -> DeprecatedTypealiasByAnnotation(typeAliasDescriptor, this)
    else -> this
}

internal fun createDeprecationDiagnostic(
        element: PsiElement, deprecation: Deprecation, languageVersionSettings: LanguageVersionSettings
): Diagnostic {
    val targetOriginal = deprecation.target.original
    return when (deprecation) {
        is DeprecatedBySinceKotlinInfo -> {
            val factory = when (deprecation.deprecationLevel) {
                WARNING -> Errors.SINCE_KOTLIN_INFO_DEPRECATION
                ERROR, HIDDEN -> Errors.SINCE_KOTLIN_INFO_DEPRECATION_ERROR
            }
            factory.on(element, targetOriginal, deprecation.sinceKotlinVersion,
                       languageVersionSettings.languageVersion to deprecation.message)
        }

        is DeprecatedTypealiasByAnnotation -> {
            val factory = when (deprecation.deprecationLevel) {
                WARNING -> Errors.TYPEALIAS_EXPANSION_DEPRECATION
                ERROR, HIDDEN -> Errors.TYPEALIAS_EXPANSION_DEPRECATION_ERROR
            }
            factory.on(element, deprecation.typeAliasTarget.original, deprecation.nested.target.original, deprecation.nested.message ?: "")
        }

        else -> {
            val factory = when (deprecation.deprecationLevel) {
                WARNING -> Errors.DEPRECATION
                ERROR, HIDDEN -> Errors.DEPRECATION_ERROR
            }
            factory.on(element, targetOriginal, deprecation.message ?: "")
        }
    }
}

// values from kotlin.DeprecationLevel
enum class DeprecationLevelValue {
    WARNING, ERROR, HIDDEN
}

class DeprecationResolver(
        storageManager: StorageManager,
        private val languageVersionSettings: LanguageVersionSettings
) {
    private val deprecations = storageManager.createMemoizedFunction { descriptor: DeclarationDescriptor ->
        val deprecations = descriptor.getOwnDeprecations()
        when {
            deprecations.isNotEmpty() -> deprecations
            descriptor is CallableMemberDescriptor -> listOfNotNull(deprecationByOverridden(descriptor))
            else -> emptyList()
        }
    }

    private val isHiddenBecauseOfKotlinVersionAccessibility = storageManager.createMemoizedFunction { descriptor: DeclarationDescriptor ->
        descriptor.checkSinceKotlinVersionAccessibility(languageVersionSettings)
    }

    fun getDeprecations(
            descriptor: DeclarationDescriptor
    ) = deprecations(descriptor.original)


    fun isDeprecatedHidden(descriptor: DeclarationDescriptor): Boolean =
            getDeprecations(descriptor).any { it.deprecationLevel == HIDDEN }

    @JvmOverloads
    fun isHiddenInResolution(
            descriptor: DeclarationDescriptor,
            isSuperCall: Boolean = false
    ): Boolean {
        if (descriptor is FunctionDescriptor) {
            if (descriptor.isHiddenToOvercomeSignatureClash) return true
            if (descriptor.isHiddenForResolutionEverywhereBesideSupercalls && !isSuperCall) return true
        }

        if (!isHiddenBecauseOfKotlinVersionAccessibility(descriptor.original)) return true

        return isDeprecatedHidden(descriptor)
    }

    private fun KotlinType.deprecationsByConstituentTypes(): List<Deprecation> =
            SmartList<Deprecation>().also { deprecations ->
                TypeUtils.contains(this) { type ->
                    type.constructor.declarationDescriptor?.let {
                        deprecations.addAll(getDeprecations(it))
                    }
                    false
                }
            }

    private fun deprecationByOverridden(root: CallableMemberDescriptor): Deprecation? {
        val visited = HashSet<CallableMemberDescriptor>()
        val deprecations = LinkedHashSet<Deprecation>()
        var hasUndeprecatedOverridden = false

        fun traverse(node: CallableMemberDescriptor) {
            if (node in visited) return

            visited.add(node)

            val deprecationsByAnnotation = node.getOwnDeprecations()
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

    private fun DeclarationDescriptor.getOwnDeprecations(): List<Deprecation> {
        // The problem is that declaration `mod` in built-ins has @Deprecated annotation but actually it was deprecated only in version 1.1
        if (this is FunctionDescriptor && this.isOperatorMod() && KotlinBuiltIns.isUnderKotlinPackage(this)) {
            if (!shouldWarnAboutDeprecatedModFromBuiltIns(languageVersionSettings)) {
                return emptyList()
            }
        }

        val result = SmartList<Deprecation>()

        fun addDeprecationIfPresent(target: DeclarationDescriptor) {
            val annotation = target.annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.deprecated)
                             ?: target.annotations.findAnnotation(JAVA_DEPRECATED)
            if (annotation != null) {
                val deprecatedByAnnotation = DeprecatedByAnnotation(annotation, target)
                val deprecation = when (target) {
                    is TypeAliasConstructorDescriptor -> DeprecatedTypealiasByAnnotation(target.typeAliasDescriptor, deprecatedByAnnotation)
                    else -> deprecatedByAnnotation
                }
                result.add(deprecation)
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
                expandedType.deprecationsByConstituentTypes().mapTo(result) { it.wrapInTypeAliasExpansion(this) }
            }
            is DescriptorDerivedFromTypeAlias -> {
                result.addAll(typeAliasDescriptor.getOwnDeprecations())
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
}
