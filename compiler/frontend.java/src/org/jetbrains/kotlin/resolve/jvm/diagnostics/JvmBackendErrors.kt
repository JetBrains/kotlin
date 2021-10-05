/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.*
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers.STRING
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.utils.join

object JvmBackendErrors {
    // TODO: slightly different errors of this type exist also in fir checkers, consider unifying
    val CONFLICTING_JVM_DECLARATIONS by error1<PsiElement, ConflictingJvmDeclarationsData>(DECLARATION_SIGNATURE_OR_DEFAULT)
    val CONFLICTING_INHERITED_JVM_DECLARATIONS by error1<PsiElement, ConflictingJvmDeclarationsData>(DECLARATION_SIGNATURE_OR_DEFAULT)
    val ACCIDENTAL_OVERRIDE by error1<PsiElement, ConflictingJvmDeclarationsData>(DECLARATION_SIGNATURE_OR_DEFAULT)

    val TYPEOF_SUSPEND_TYPE by error0<PsiElement>()
    val TYPEOF_EXTENSION_FUNCTION_TYPE by error0<PsiElement>()
    val TYPEOF_ANNOTATED_TYPE by error0<PsiElement>()
    val TYPEOF_NON_REIFIED_TYPE_PARAMETER_WITH_RECURSIVE_BOUND by error1<PsiElement, String>()

    val SUSPENSION_POINT_INSIDE_MONITOR by error1<PsiElement, String>()

    init {
        RootDiagnosticRendererFactory.registerFactory(KtDefaultJvmErrorMessages)
    }
}

object KtDefaultJvmErrorMessages : BaseDiagnosticRendererFactory() {

    @JvmField
    val CONFLICTING_JVM_DECLARATIONS_DATA = Renderer<ConflictingJvmDeclarationsData> {
        val renderedDescriptors: List<DeclarationDescriptor?> =
            it.signatureOrigins.mapNotNull(
                JvmDeclarationOrigin::descriptor
            ).sortedWith(MemberComparator.INSTANCE)
        val renderingContext: RenderingContext =
            RenderingContext.Impl(renderedDescriptors)
        """
                The following declarations have the same JVM signature (${it.signature.name}${it.signature.desc}):
                
                """.trimIndent() +
                join(renderedDescriptors.map { descriptor: DeclarationDescriptor? ->
                    "    " + Renderers.WITHOUT_MODIFIERS.render(
                        descriptor!!, renderingContext
                    )
                }, "\n")
    }

    override val MAP = KtDiagnosticFactoryToRendererMap("KT").also { map ->
        map.put(JvmBackendErrors.CONFLICTING_JVM_DECLARATIONS, "Platform declaration clash: {0}", CONFLICTING_JVM_DECLARATIONS_DATA)
        map.put(JvmBackendErrors.ACCIDENTAL_OVERRIDE, "Accidental override: {0}", CONFLICTING_JVM_DECLARATIONS_DATA)
        map.put(JvmBackendErrors.CONFLICTING_INHERITED_JVM_DECLARATIONS, "Inherited platform declarations clash: {0}", CONFLICTING_JVM_DECLARATIONS_DATA)
        map.put(JvmBackendErrors.TYPEOF_SUSPEND_TYPE, "Suspend functional types are not supported in typeOf")
        map.put(JvmBackendErrors.TYPEOF_EXTENSION_FUNCTION_TYPE, "Extension function types are not supported in typeOf")
        map.put(JvmBackendErrors.TYPEOF_ANNOTATED_TYPE, "Annotated types are not supported in typeOf")
        map.put(JvmBackendErrors.TYPEOF_NON_REIFIED_TYPE_PARAMETER_WITH_RECURSIVE_BOUND, "Non-reified type parameters with recursive bounds are not supported yet: {0}", STRING)
        map.put(JvmBackendErrors.SUSPENSION_POINT_INSIDE_MONITOR, "A suspension point at {0} is inside a critical section", STRING)
    }
}
