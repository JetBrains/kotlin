/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.utils.join

object KtErrorsJvm {
    // TODO: slightly different errors of this type exist also in fir checkers, consider unifying
    val CONFLICTING_JVM_DECLARATIONS by error1<PsiElement, ConflictingJvmDeclarationsData>()
    val CONFLICTING_INHERITED_JVM_DECLARATIONS by error1<PsiElement, ConflictingJvmDeclarationsData>()
    val ACCIDENTAL_OVERRIDE by error1<PsiElement, ConflictingJvmDeclarationsData>()
}

class KtDefaultJvmErrorMessages {
    companion object {

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

        val MAP = KtDiagnosticFactoryToRendererMap("KT").also { map ->
            map.put(KtErrorsJvm.CONFLICTING_JVM_DECLARATIONS, "Platform declaration clash: {0}", CONFLICTING_JVM_DECLARATIONS_DATA)
            map.put(KtErrorsJvm.ACCIDENTAL_OVERRIDE, "Accidental override: {0}", CONFLICTING_JVM_DECLARATIONS_DATA)
            map.put(KtErrorsJvm.CONFLICTING_INHERITED_JVM_DECLARATIONS, "Inherited platform declarations clash: {0}", CONFLICTING_JVM_DECLARATIONS_DATA)
        }
    }
}
