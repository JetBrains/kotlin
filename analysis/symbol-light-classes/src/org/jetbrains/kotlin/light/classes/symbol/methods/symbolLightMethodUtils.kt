/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmExposeBoxedAnnotation
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.name.JvmStandardClassIds

internal fun isSuppressedFinalModifier(string: String, containingClass: SymbolLightClassBase, symbol: KaCallableSymbol): Boolean {
    return string == PsiModifier.FINAL && (containingClass.isEnum && symbol.origin == KaSymbolOrigin.SOURCE_MEMBER_GENERATED || containingClass.isInterface)
}

/**
 * Whether [JvmExposeBoxed] feature is enabled.
 */
internal enum class JvmExposeBoxedMode {
    /** Explicit [JvmExposeBoxed] annotation on the declaration */
    EXPLICIT,

    /**
     * The [LanguageFeature.ImplicitJvmExposeBoxed] feature is enabled or
     * the containing class is marked with [JvmExposeBoxed] annotation
     */
    IMPLICIT,

    /** The feature is disabled for the declaration */
    NONE;
}

/**
 * [JvmExposeBoxedMode] mode for a [callableSymbol].
 *
 * Note: it doesn't work properly for property accessors.
 *
 * @see JvmExposeBoxedMode
 * @see hasJvmExposeBoxedAnnotation
 */
internal fun KaSession.jvmExposeBoxedMode(callableSymbol: KaCallableSymbol): JvmExposeBoxedMode {
    if (callableSymbol.hasJvmExposeBoxedAnnotation()) {
        return JvmExposeBoxedMode.EXPLICIT
    }

    val containingClass = callableSymbol.containingDeclaration as? KaClassSymbol
    if (containingClass != null && JvmStandardClassIds.JVM_EXPOSE_BOXED_ANNOTATION_CLASS_ID in containingClass.annotations) {
        return JvmExposeBoxedMode.IMPLICIT
    }

    val module = containingClass?.containingModule ?: callableSymbol.containingModule
    val isFeatureEnabled = when (module) {
        is KaSourceModule -> module.languageVersionSettings.supportsFeature(LanguageFeature.ImplicitJvmExposeBoxed)
        is KaScriptModule -> module.languageVersionSettings.supportsFeature(LanguageFeature.ImplicitJvmExposeBoxed)
        else -> false
    }

    return if (isFeatureEnabled) JvmExposeBoxedMode.IMPLICIT else JvmExposeBoxedMode.NONE
}

internal class MethodGenerationResult(val isRegularMethodRequired: Boolean, val isBoxedMethodRequired: Boolean) {
    val isAnyMethodRequired: Boolean get() = isRegularMethodRequired || isBoxedMethodRequired
}

/**
 * Analyzes the requirement for regular and boxed method generation based on given parameters.
 *
 * @param exposeBoxedMode The [JvmExposeBoxedMode] for the method.
 * @param hasValueClassInParameterType Whether any of the method's parameters contain a value class.
 * @param hasValueClassInReturnType Whether the method's return type is a value class.
 * @param isAffectedByValueClass Whether the method's name is mangled due to value classes, or the method is declared inside a value class and not materialized.
 * @param hasJvmNameAnnotation Whether the method has a [JvmName] annotation.
 * @param isOverridable Whether the method can be overridden.
 */
internal fun methodGeneration(
    exposeBoxedMode: JvmExposeBoxedMode,
    hasValueClassInParameterType: Boolean,
    hasValueClassInReturnType: Boolean,
    isAffectedByValueClass: Boolean,
    hasJvmNameAnnotation: Boolean,
    isSuspend: Boolean,
    isOverridable: Boolean,
): MethodGenerationResult {
    var isBoxedAccessorRequired = false
    var isRegularAccessorRequired = false

    // Explicit mode -> a boxed method is requested (even if it is a JVM name clash)
    if (exposeBoxedMode == JvmExposeBoxedMode.EXPLICIT && (hasValueClassInParameterType || hasValueClassInReturnType || isAffectedByValueClass)) {
        isBoxedAccessorRequired = true
    }

    if (isAffectedByValueClass) {
        // JvmName -> unmangled method can be generated
        isRegularAccessorRequired = hasJvmNameAnnotation

        isBoxedAccessorRequired = when {
            // The check already performed by the explicit mode
            isBoxedAccessorRequired -> true

            // No implicit feature -> no boxed methods can be auto-generated
            exposeBoxedMode != JvmExposeBoxedMode.IMPLICIT -> false

            // Suspend function -> no boxed methods can be auto-generated
            isSuspend -> false

            // In interface or in open class -> no boxed methods can be auto-generated. @JvmName problem
            isOverridable -> false

            // No JvmName -> the default method has a mangled name, so the boxed method can be generated
            !hasJvmNameAnnotation -> true

            // At least one parameter has a value class -> the boxed method won't lead to a JVM name clash
            else -> hasValueClassInParameterType
        }
    } else {
        // Unmangled name -> regular method is needed
        isRegularAccessorRequired = true
    }

    return MethodGenerationResult(
        isRegularMethodRequired = isRegularAccessorRequired,
        isBoxedMethodRequired = isBoxedAccessorRequired,
    )
}

internal fun KaDeclarationSymbol.isOverridable(): Boolean =
    visibility != KaSymbolVisibility.PRIVATE && modality != KaSymbolModality.FINAL