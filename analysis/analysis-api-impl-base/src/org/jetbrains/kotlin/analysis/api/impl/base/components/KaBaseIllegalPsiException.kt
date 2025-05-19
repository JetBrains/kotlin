/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.utils.errors.withKaModuleEntry
import org.jetbrains.kotlin.utils.exceptions.KotlinIllegalArgumentExceptionWithAttachments
import org.jetbrains.kotlin.utils.exceptions.buildAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

/**
 * Exception thrown when a PSI element cannot be analyzed in the current session.
 *
 * @see KaSession
 */
@KaImplementationDetail
class KaBaseIllegalPsiException private constructor(
    useSiteModule: KaModule,
    psiModule: KaModule,
    psi: PsiElement,
) : KotlinIllegalArgumentExceptionWithAttachments(
    "The element cannot be analyzed in the context of the current session.\n" +
            "The call site should be adjusted according to ${KaSession::class.simpleName} KDoc.\n" +
            "Use site module class: ${useSiteModule::class.simpleName}\n" +
            "PSI module class: ${psiModule::class.simpleName}\n" +
            "PSI element class: ${psi::class.simpleName}",
) {
    init {
        buildAttachment("info.txt") {
            withKaModuleEntry("useSiteModule", useSiteModule)
            withKaModuleEntry("psiModule", psiModule)

            runCatching {
                withPsiEntry("psi", psi)
            }.exceptionOrNull()?.let {
                withEntry("psiException", it.stackTraceToString())
            }
        }
    }

    companion object {
        fun create(session: KaSession, psi: PsiElement): KaBaseIllegalPsiException = with(session) {
            val psiModule = getModule(psi)
            KaBaseIllegalPsiException(useSiteModule, psiModule, psi)
        }

        /**
         * This is a temporary solution to allow accessing PSI elements from unrelated [KaSession] to allow usages for old places
         * and forbit incorrect behavior in new places.
         */
        @KaImplementationDetail
        @KaIdeApi
        @Suppress("unused")
        fun <T> allowIllegalPsiAccess(action: () -> T): T {
            val old = allowIllegalPsiAccess.get()
            allowIllegalPsiAccess.set(true)
            return try {
                action()
            } finally {
                allowIllegalPsiAccess.set(old)
            }
        }
    }
}

private val allowIllegalPsiAccess = ThreadLocal.withInitial { false }

@KaImplementationDetail
context(component: KaBaseSessionComponent<S>)
fun <S : KaSession> PsiElement.checkValidity() = with(component.analysisSession) {
    if (!canBeAnalysed() && Registry.`is`("kotlin.analysis.validate.psi.input", true) && !allowIllegalPsiAccess.get()) {
        throw KaBaseIllegalPsiException.create(this, this@checkValidity)
    }
}

@KaImplementationDetail
context(component: KaBaseSessionComponent<S>)
@JvmName("withPsiValidityAssertionAsReceiver")
inline fun <S : KaSession, R> PsiElement?.withPsiValidityAssertion(builder: () -> R): R = component.withValidityAssertion {
    this?.checkValidity()
    builder()
}

@KaImplementationDetail
context(component: KaBaseSessionComponent<S>)
inline fun <S : KaSession, R> withPsiValidityAssertion(
    element: PsiElement?,
    builder: () -> R,
): R = element.withPsiValidityAssertion(builder)

@KaImplementationDetail
context(component: KaBaseSessionComponent<S>)
inline fun <S : KaSession, R> withPsiValidityAssertion(
    vararg elements: PsiElement?,
    builder: () -> R,
): R = component.withValidityAssertion {
    for (element in elements) {
        element?.checkValidity()
    }

    builder()
}

@KaImplementationDetail
context(component: KaBaseSessionComponent<S>)
inline fun <S : KaSession, R> withPsiValidityAssertion(
    elements: Iterable<PsiElement?>,
    builder: () -> R,
): R = component.withValidityAssertion {
    for (element in elements) {
        element?.checkValidity()
    }

    builder()
}
