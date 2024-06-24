/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.diagnostics.*

internal interface KaFirDiagnosticCreator

internal fun interface KaFirDiagnostic0Creator : KaFirDiagnosticCreator {
    fun KaFirSession.create(diagnostic: KtSimpleDiagnostic): KaFirDiagnostic<*>
}

internal fun interface KaFirDiagnostic1Creator<A> : KaFirDiagnosticCreator {
    fun KaFirSession.create(diagnostic: KtDiagnosticWithParameters1<A>): KaFirDiagnostic<*>
}

internal fun interface KaFirDiagnostic2Creator<A, B> : KaFirDiagnosticCreator {
    fun KaFirSession.create(diagnostic: KtDiagnosticWithParameters2<A, B>): KaFirDiagnostic<*>
}

internal fun interface KaFirDiagnostic3Creator<A, B, C> : KaFirDiagnosticCreator {
    fun KaFirSession.create(diagnostic: KtDiagnosticWithParameters3<A, B, C>): KaFirDiagnostic<*>
}

internal fun interface KaFirDiagnostic4Creator<A, B, C, D> : KaFirDiagnosticCreator {
    fun KaFirSession.create(diagnostic: KtDiagnosticWithParameters4<A, B, C, D>): KaFirDiagnostic<*>
}

internal class KaDiagnosticConverter(private val conversions: Map<AbstractKtDiagnosticFactory, KaFirDiagnosticCreator>) {
    fun convert(analysisSession: KaFirSession, diagnostic: KtDiagnostic): KaFirDiagnostic<*> {
        val creator = conversions[diagnostic.factory] ?: buildCreatorForPluginDiagnostic(diagnostic.factory)

        @Suppress("UNCHECKED_CAST")
        return with(analysisSession) {
            when (creator) {
                is KaFirDiagnostic0Creator -> with(creator) {
                    create(diagnostic as KtSimpleDiagnostic)
                }
                is KaFirDiagnostic1Creator<*> -> with(creator as KaFirDiagnostic1Creator<Any?>) {
                    create(diagnostic as KtDiagnosticWithParameters1<Any?>)
                }
                is KaFirDiagnostic2Creator<*, *> -> with(creator as KaFirDiagnostic2Creator<Any?, Any?>) {
                    create(diagnostic as KtDiagnosticWithParameters2<Any?, Any?>)
                }
                is KaFirDiagnostic3Creator<*, *, *> -> with(creator as KaFirDiagnostic3Creator<Any?, Any?, Any?>) {
                    create(diagnostic as KtDiagnosticWithParameters3<Any?, Any?, Any?>)
                }
                is KaFirDiagnostic4Creator<*, *, *, *> -> with(creator as KaFirDiagnostic4Creator<Any?, Any?, Any?, Any?>) {
                    create(diagnostic as KtDiagnosticWithParameters4<Any?, Any?, Any?, Any?>)
                }
                else -> error("Invalid KtFirDiagnosticCreator ${creator::class.simpleName}")
            }
        }
    }

    @Suppress("RemoveExplicitTypeArguments") // See KT-52838
    private fun buildCreatorForPluginDiagnostic(factory: AbstractKtDiagnosticFactory): KaFirDiagnosticCreator {
        return when (factory) {
            is KtDiagnosticFactory0 -> KaFirDiagnostic0Creator {
                KaCompilerPluginDiagnostic0Impl(it as KtPsiSimpleDiagnostic, token)
            }
            is KtDiagnosticFactory1<*> -> KaFirDiagnostic1Creator<Any?> { // Type argument specified because of KT-55281
                KaCompilerPluginDiagnostic1Impl(
                    it as KtPsiDiagnosticWithParameters1<*>,
                    token,
                    convertArgument(it.a, this)
                )
            }
            is KtDiagnosticFactory2<*, *> -> KaFirDiagnostic2Creator<Any?, Any?> {
                KaCompilerPluginDiagnostic2Impl(
                    it as KtPsiDiagnosticWithParameters2<*, *>,
                    token,
                    convertArgument(it.a, this),
                    convertArgument(it.b, this)
                )
            }
            is KtDiagnosticFactory3<*, *, *> -> KaFirDiagnostic3Creator<Any?, Any?, Any?> {
                KaCompilerPluginDiagnostic3Impl(
                    it as KtPsiDiagnosticWithParameters3<*, *, *>,
                    token,
                    convertArgument(it.a, this),
                    convertArgument(it.b, this),
                    convertArgument(it.c, this)
                )
            }
            is KtDiagnosticFactory4<*, *, *, *> -> KaFirDiagnostic4Creator<Any?, Any?, Any?, Any?> {
                KaCompilerPluginDiagnostic4Impl(
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

internal class KaDiagnosticConverterBuilder private constructor() {
    private val conversions = mutableMapOf<AbstractKtDiagnosticFactory, KaFirDiagnosticCreator>()

    fun add(diagnostic: KtDiagnosticFactory0, creator: KaFirDiagnostic0Creator) {
        conversions[diagnostic] = creator
    }

    fun <A> add(diagnostic: KtDiagnosticFactory1<A>, creator: KaFirDiagnostic1Creator<A>) {
        conversions[diagnostic] = creator
    }

    fun <A, B> add(diagnostic: KtDiagnosticFactory2<A, B>, creator: KaFirDiagnostic2Creator<A, B>) {
        conversions[diagnostic] = creator
    }

    fun <A, B, C> add(diagnostic: KtDiagnosticFactory3<A, B, C>, creator: KaFirDiagnostic3Creator<A, B, C>) {
        conversions[diagnostic] = creator
    }

    fun <A, B, C, D> add(diagnostic: KtDiagnosticFactory4<A, B, C, D>, creator: KaFirDiagnostic4Creator<A, B, C, D>) {
        conversions[diagnostic] = creator
    }

    private fun build() = KaDiagnosticConverter(conversions)

    companion object {
        inline fun buildConverter(init: KaDiagnosticConverterBuilder.() -> Unit) =
            KaDiagnosticConverterBuilder().apply(init).build()
    }
}
