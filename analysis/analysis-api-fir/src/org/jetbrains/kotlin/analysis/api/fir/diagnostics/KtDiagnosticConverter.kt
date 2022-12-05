/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.diagnostics.*

internal interface KtFirDiagnosticCreator

internal fun interface KtFirDiagnostic0Creator : KtFirDiagnosticCreator {
    fun KtFirAnalysisSession.create(diagnostic: KtSimpleDiagnostic): KtFirDiagnostic<*>
}

internal fun interface KtFirDiagnostic1Creator<A> : KtFirDiagnosticCreator {
    fun KtFirAnalysisSession.create(diagnostic: KtDiagnosticWithParameters1<A>): KtFirDiagnostic<*>
}

internal fun interface KtFirDiagnostic2Creator<A, B> : KtFirDiagnosticCreator {
    fun KtFirAnalysisSession.create(diagnostic: KtDiagnosticWithParameters2<A, B>): KtFirDiagnostic<*>
}

internal fun interface KtFirDiagnostic3Creator<A, B, C> : KtFirDiagnosticCreator {
    fun KtFirAnalysisSession.create(diagnostic: KtDiagnosticWithParameters3<A, B, C>): KtFirDiagnostic<*>
}

internal fun interface KtFirDiagnostic4Creator<A, B, C, D> : KtFirDiagnosticCreator {
    fun KtFirAnalysisSession.create(diagnostic: KtDiagnosticWithParameters4<A, B, C, D>): KtFirDiagnostic<*>
}

internal class KtDiagnosticConverter(private val conversions: Map<AbstractKtDiagnosticFactory, KtFirDiagnosticCreator>) {
    fun convert(analysisSession: KtFirAnalysisSession, diagnostic: KtDiagnostic): KtFirDiagnostic<*> {
        val creator = conversions[diagnostic.factory] ?: buildCreatorForPluginDiagnostic(diagnostic.factory)

        @Suppress("UNCHECKED_CAST")
        return with(analysisSession) {
            when (creator) {
                is KtFirDiagnostic0Creator -> with(creator) {
                    create(diagnostic as KtSimpleDiagnostic)
                }
                is KtFirDiagnostic1Creator<*> -> with(creator as KtFirDiagnostic1Creator<Any?>) {
                    create(diagnostic as KtDiagnosticWithParameters1<Any?>)
                }
                is KtFirDiagnostic2Creator<*, *> -> with(creator as KtFirDiagnostic2Creator<Any?, Any?>) {
                    create(diagnostic as KtDiagnosticWithParameters2<Any?, Any?>)
                }
                is KtFirDiagnostic3Creator<*, *, *> -> with(creator as KtFirDiagnostic3Creator<Any?, Any?, Any?>) {
                    create(diagnostic as KtDiagnosticWithParameters3<Any?, Any?, Any?>)
                }
                is KtFirDiagnostic4Creator<*, *, *, *> -> with(creator as KtFirDiagnostic4Creator<Any?, Any?, Any?, Any?>) {
                    create(diagnostic as KtDiagnosticWithParameters4<Any?, Any?, Any?, Any?>)
                }
                else -> error("Invalid KtFirDiagnosticCreator ${creator::class.simpleName}")
            }
        }
    }

    @Suppress("RemoveExplicitTypeArguments") // See KT-52838
    private fun buildCreatorForPluginDiagnostic(factory: AbstractKtDiagnosticFactory): KtFirDiagnosticCreator {
        return when (factory) {
            is KtDiagnosticFactory0 -> KtFirDiagnostic0Creator {
                KtCompilerPluginDiagnostic0Impl(it as KtPsiSimpleDiagnostic, token)
            }
            is KtDiagnosticFactory1<*> -> KtFirDiagnostic1Creator<Any?> { // Type argument specified because of KT-55281
                KtCompilerPluginDiagnostic1Impl(
                    it as KtPsiDiagnosticWithParameters1<*>,
                    token,
                    convertArgument(it.a, this)
                )
            }
            is KtDiagnosticFactory2<*, *> -> KtFirDiagnostic2Creator<Any?, Any?> {
                KtCompilerPluginDiagnostic2Impl(
                    it as KtPsiDiagnosticWithParameters2<*, *>,
                    token,
                    convertArgument(it.a, this),
                    convertArgument(it.b, this)
                )
            }
            is KtDiagnosticFactory3<*, *, *> -> KtFirDiagnostic3Creator<Any?, Any?, Any?> {
                KtCompilerPluginDiagnostic3Impl(
                    it as KtPsiDiagnosticWithParameters3<*, *, *>,
                    token,
                    convertArgument(it.a, this),
                    convertArgument(it.b, this),
                    convertArgument(it.c, this)
                )
            }
            is KtDiagnosticFactory4<*, *, *, *> -> KtFirDiagnostic4Creator<Any?, Any?, Any?, Any?> {
                KtCompilerPluginDiagnostic4Impl(
                    it as KtPsiDiagnosticWithParameters4<*, *, *, *>,
                    token,
                    convertArgument(it.a, this),
                    convertArgument(it.b, this),
                    convertArgument(it.c, this),
                    convertArgument(it.d, this)
                )
            }
        }
    }
}

internal class KtDiagnosticConverterBuilder private constructor() {
    private val conversions = mutableMapOf<AbstractKtDiagnosticFactory, KtFirDiagnosticCreator>()

    fun add(diagnostic: KtDiagnosticFactory0, creator: KtFirDiagnostic0Creator) {
        conversions[diagnostic] = creator
    }

    fun <A> add(diagnostic: KtDiagnosticFactory1<A>, creator: KtFirDiagnostic1Creator<A>) {
        conversions[diagnostic] = creator
    }

    fun <A, B> add(diagnostic: KtDiagnosticFactory2<A, B>, creator: KtFirDiagnostic2Creator<A, B>) {
        conversions[diagnostic] = creator
    }

    fun <A, B, C> add(diagnostic: KtDiagnosticFactory3<A, B, C>, creator: KtFirDiagnostic3Creator<A, B, C>) {
        conversions[diagnostic] = creator
    }

    fun <A, B, C, D> add(diagnostic: KtDiagnosticFactory4<A, B, C, D>, creator: KtFirDiagnostic4Creator<A, B, C, D>) {
        conversions[diagnostic] = creator
    }

    private fun build() = KtDiagnosticConverter(conversions)

    companion object {
        inline fun buildConverter(init: KtDiagnosticConverterBuilder.() -> Unit) =
            KtDiagnosticConverterBuilder().apply(init).build()
    }
}
