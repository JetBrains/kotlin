/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession

internal interface KtFirDiagnosticCreator

internal fun interface KtFirDiagnostic0Creator : KtFirDiagnosticCreator {
    fun KtFirAnalysisSession.create(diagnostic: FirSimpleDiagnostic<*>): KtFirDiagnostic<*>
}

internal fun interface KtFirDiagnostic1Creator<A> : KtFirDiagnosticCreator {
    fun KtFirAnalysisSession.create(diagnostic: FirDiagnosticWithParameters1<*, A>): KtFirDiagnostic<*>
}

internal fun interface KtFirDiagnostic2Creator<A, B> : KtFirDiagnosticCreator {
    fun KtFirAnalysisSession.create(diagnostic: FirDiagnosticWithParameters2<*, A, B>): KtFirDiagnostic<*>
}

internal fun interface KtFirDiagnostic3Creator<A, B, C> : KtFirDiagnosticCreator {
    fun KtFirAnalysisSession.create(diagnostic: FirDiagnosticWithParameters3<*, A, B, C>): KtFirDiagnostic<*>
}

internal fun interface KtFirDiagnostic4Creator<A, B, C, D> : KtFirDiagnosticCreator {
    fun KtFirAnalysisSession.create(diagnostic: FirDiagnosticWithParameters4<*, A, B, C, D>): KtFirDiagnostic<*>
}

internal class KtDiagnosticConverter(private val conversions: Map<AbstractFirDiagnosticFactory<*, *>, KtFirDiagnosticCreator>) {
    fun convert(analysisSession: KtFirAnalysisSession, diagnostic: FirDiagnostic<*>): KtFirDiagnostic<*> {
        val creator = conversions[diagnostic.factory]
            ?: error("No conversion was found for ${diagnostic.factory}")

        @Suppress("UNCHECKED_CAST")
        return with(analysisSession) {
            when (creator) {
                is KtFirDiagnostic0Creator -> with(creator) {
                    create(diagnostic as FirSimpleDiagnostic<*>)
                }
                is KtFirDiagnostic1Creator<*> -> with(creator as KtFirDiagnostic1Creator<Any?>) {
                    create(diagnostic as FirDiagnosticWithParameters1<FirSourceElement, Any?>)
                }
                is KtFirDiagnostic2Creator<*, *> -> with(creator as KtFirDiagnostic2Creator<Any?, Any?>) {
                    create(diagnostic as FirDiagnosticWithParameters2<FirSourceElement, Any?, Any?>)
                }
                is KtFirDiagnostic3Creator<*, *, *> -> with(creator as KtFirDiagnostic3Creator<Any?, Any?, Any?>) {
                    create(diagnostic as FirDiagnosticWithParameters3<FirSourceElement, Any?, Any?, Any?>)
                }
                is KtFirDiagnostic4Creator<*, *, *, *> -> with(creator as KtFirDiagnostic4Creator<Any?, Any?, Any?, Any?>) {
                    create(diagnostic as FirDiagnosticWithParameters4<FirSourceElement, Any?, Any?, Any?, Any?>)
                }
                else -> error("Invalid KtFirDiagnosticCreator ${creator::class.simpleName}")
            }
        }
    }
}

internal class KtDiagnosticConverterBuilder private constructor() {
    private val conversions = mutableMapOf<AbstractFirDiagnosticFactory<*, *>, KtFirDiagnosticCreator>()

    fun add(diagnostic: FirDiagnosticFactory0<*>, creator: KtFirDiagnostic0Creator) {
        conversions[diagnostic] = creator
    }

    fun <A> add(diagnostic: FirDiagnosticFactory1<*, A>, creator: KtFirDiagnostic1Creator<A>) {
        conversions[diagnostic] = creator
    }

    fun <A, B> add(diagnostic: FirDiagnosticFactory2<*, A, B>, creator: KtFirDiagnostic2Creator<A, B>) {
        conversions[diagnostic] = creator
    }

    fun <A, B, C> add(diagnostic: FirDiagnosticFactory3<*, A, B, C>, creator: KtFirDiagnostic3Creator<A, B, C>) {
        conversions[diagnostic] = creator
    }

    fun <A, B, C, D> add(diagnostic: FirDiagnosticFactory4<*, A, B, C, D>, creator: KtFirDiagnostic4Creator<A, B, C, D>) {
        conversions[diagnostic] = creator
    }

    private fun build() = KtDiagnosticConverter(conversions)

    companion object {
        inline fun buildConverter(init: KtDiagnosticConverterBuilder.() -> Unit) =
            KtDiagnosticConverterBuilder().apply(init).build()
    }
}
