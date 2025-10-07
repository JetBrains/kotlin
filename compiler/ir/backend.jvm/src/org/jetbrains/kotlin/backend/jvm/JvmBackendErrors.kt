/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.CLASS_ID
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers.NAME
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers.STRING
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

object JvmBackendErrors : KtDiagnosticsContainer() {
    val CONFLICTING_JVM_DECLARATIONS by error1<PsiElement, String>(DECLARATION_SIGNATURE_OR_DEFAULT)
    val CONFLICTING_INHERITED_JVM_DECLARATIONS by error1<PsiElement, String>(DECLARATION_SIGNATURE_OR_DEFAULT)
    val ACCIDENTAL_OVERRIDE by error1<PsiElement, String>(DECLARATION_SIGNATURE_OR_DEFAULT)
    val ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD by deprecationError1<PsiElement, String>(
        LanguageFeature.ForbidBridgesConflictingWithInheritedMethodsInJvmCode,
        DECLARATION_SIGNATURE_OR_DEFAULT
    )

    val TYPEOF_SUSPEND_TYPE by error0<PsiElement>()
    val TYPEOF_NON_REIFIED_TYPE_PARAMETER_WITH_RECURSIVE_BOUND by error1<PsiElement, String>()

    val SUSPENSION_POINT_INSIDE_MONITOR by error1<PsiElement, String>()

    val SCRIPT_CAPTURING_NESTED_CLASS by error2<PsiElement, String, String>()
    val SCRIPT_CAPTURING_OBJECT by error1<PsiElement, String>()
    val SCRIPT_CAPTURING_INTERFACE by error1<PsiElement, String>()
    val SCRIPT_CAPTURING_ENUM by error1<PsiElement, String>()
    val SCRIPT_CAPTURING_ENUM_ENTRY by error1<PsiElement, String>()

    val INLINE_CALL_CYCLE by error1<PsiElement, Name>()

    val NOT_ALL_MULTIFILE_CLASS_PARTS_ARE_JVM_SYNTHETIC by error0<PsiElement>()

    val DUPLICATE_CLASS_NAMES by error2<PsiElement, String, String>()

    val PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION by deprecationError1<PsiElement, ClassId>(LanguageFeature.ForbidExposingLessVisibleTypesInInline)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory {
        return KtDefaultJvmErrorMessages
    }
}

object KtDefaultJvmErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("KT") { map ->
        map.put(JvmBackendErrors.CONFLICTING_JVM_DECLARATIONS, "Platform declaration clash: {0}", STRING)
        map.put(JvmBackendErrors.ACCIDENTAL_OVERRIDE, "Accidental override: {0}", STRING)
        map.put(JvmBackendErrors.ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD, "Accidental override by bridge method: {0}", STRING)
        map.put(JvmBackendErrors.CONFLICTING_INHERITED_JVM_DECLARATIONS, "Inherited platform declarations clash: {0}", STRING)
        map.put(JvmBackendErrors.TYPEOF_SUSPEND_TYPE, "Suspend functional types are not supported in typeOf")
        map.put(JvmBackendErrors.TYPEOF_NON_REIFIED_TYPE_PARAMETER_WITH_RECURSIVE_BOUND, "Non-reified type parameters with recursive bounds are not supported yet: {0}", STRING)
        map.put(JvmBackendErrors.SUSPENSION_POINT_INSIDE_MONITOR, "A suspension point at {0} is inside a critical section", STRING)

        map.put(JvmBackendErrors.SCRIPT_CAPTURING_NESTED_CLASS, "Nested class {0} captures the script class instance. Try to use explicit inner modifier for both nested {0} and outer {1}", STRING, STRING)
        map.put(JvmBackendErrors.SCRIPT_CAPTURING_OBJECT, "Object {0} captures the script class instance. Try to use class or anonymous object instead", STRING)
        map.put(JvmBackendErrors.SCRIPT_CAPTURING_INTERFACE, "Interface {0} captures the script class instance. Try to use class instead", STRING)
        map.put(JvmBackendErrors.SCRIPT_CAPTURING_ENUM, "Enum class {0} captures the script class instance. Try to use class or anonymous object instead", STRING)
        map.put(JvmBackendErrors.SCRIPT_CAPTURING_ENUM_ENTRY, "Enum entry {0} captures the script class instance. Try to use class or anonymous object instead", STRING)

        map.put(JvmBackendErrors.INLINE_CALL_CYCLE, "The ''{0}'' invocation is a part of inline cycle", NAME)
        map.put(
            JvmBackendErrors.NOT_ALL_MULTIFILE_CLASS_PARTS_ARE_JVM_SYNTHETIC,
            "All of multi-file class parts should be annotated with @JvmSynthetic if at least one of them is"
        )

        map.put(JvmBackendErrors.DUPLICATE_CLASS_NAMES, "Duplicate JVM class name ''{0}'' generated from: {1}", STRING, STRING)

        map.put(
            JvmBackendErrors.PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION,
            "Non-private inline function accesses a private class ''{0}''", CLASS_ID,
        )
    }
}
