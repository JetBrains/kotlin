/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.config.JvmAnalysisFlags
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
     * The [JvmAnalysisFlags.implicitJvmExposeBoxed] feature is enabled or
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
        is KaSourceModule -> module.languageVersionSettings.getFlag(JvmAnalysisFlags.implicitJvmExposeBoxed)
        is KaScriptModule -> module.languageVersionSettings.getFlag(JvmAnalysisFlags.implicitJvmExposeBoxed)
        else -> false
    }

    return if (isFeatureEnabled) JvmExposeBoxedMode.IMPLICIT else JvmExposeBoxedMode.NONE
}

internal class MethodGenerationResult(val isRegularMethodRequired: Boolean, val isBoxedMethodRequired: Boolean) {
    val isAnyMethodRequired: Boolean get() = isRegularMethodRequired || isBoxedMethodRequired
}

/**
 * Whether [symbol] is effectively private: either it is `private` itself, or it is a member
 * (transitively) of a `private` class.
 *
 * Mirrors `org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate` used by the JVM backend so that
 * light classes do not autogenerate `@JvmExposeBoxed` boxed variants for declarations that cannot be
 * accessed from Java anyway.
 */
context(_: KaSession)
internal fun isEffectivelyPrivate(symbol: KaDeclarationSymbol): Boolean {
    if (symbol.visibility == KaSymbolVisibility.PRIVATE) return true
    val containingClass = symbol.containingDeclaration as? KaClassSymbol ?: return false
    return isEffectivelyPrivate(containingClass)
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
 * @param isEffectivelyPrivate Whether the method is effectively private and therefore must not be exposed. @see isEffectivelyPrivate
 */
internal fun methodGeneration(
    exposeBoxedMode: JvmExposeBoxedMode,
    hasValueClassInParameterType: Boolean,
    hasValueClassInReturnType: Boolean,
    isAffectedByValueClass: Boolean,
    hasJvmNameAnnotation: Boolean,
    isSuspend: Boolean,
    isOverridable: Boolean,
    isEffectivelyPrivate: Boolean,
): MethodGenerationResult {
    var isBoxedAccessorRequired = false
    var isRegularAccessorRequired = false

    // Explicit mode -> a boxed method is requested (even if it is a JVM name clash)
    if (exposeBoxedMode == JvmExposeBoxedMode.EXPLICIT &&
        !isEffectivelyPrivate &&
        (hasValueClassInParameterType || hasValueClassInReturnType || isAffectedByValueClass)
    ) {
        isBoxedAccessorRequired = true
    }

    if (isAffectedByValueClass) {
        // JvmName -> unmangled method can be generated
        isRegularAccessorRequired = hasJvmNameAnnotation

        isBoxedAccessorRequired = when {
            // The check already performed by the explicit mode
            isBoxedAccessorRequired -> true

            // Private declarations are inaccessible from Java -> no boxed methods can be auto-generated
            isEffectivelyPrivate -> false

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
