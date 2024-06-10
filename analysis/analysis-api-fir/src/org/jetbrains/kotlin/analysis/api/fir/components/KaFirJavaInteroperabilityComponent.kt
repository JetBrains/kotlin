/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaJavaInteroperabilityComponent
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.isTopLevel
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.jvmClassNameIfDeserialized
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.psi.KtFile

internal class KaFirJavaInteroperabilityComponent(
    override val analysisSessionProvider: () -> KaFirSession,
    override val token: KaLifetimeToken
) : KaSessionComponent<KaFirSession>(), KaJavaInteroperabilityComponent {
    override val KaCallableSymbol.containingJvmClassName: String?
        get() = withValidityAssertion {
            val symbol = this@containingJvmClassName

            with(analysisSession) {
                val platform = symbol.containingModule.platform
                if (!platform.has<JvmPlatform>()) return null

                val containingSymbolOrSelf = when (symbol) {
                    is KaValueParameterSymbol -> {
                        symbol.containingSymbol as? KaFunctionLikeSymbol ?: symbol
                    }
                    is KaPropertyAccessorSymbol -> {
                        symbol.containingSymbol as? KaPropertySymbol ?: symbol
                    }
                    is KaBackingFieldSymbol -> symbol.owningProperty
                    else -> symbol
                }

                val firSymbol = containingSymbolOrSelf.firSymbol

                firSymbol.jvmClassNameIfDeserialized()?.let {
                    return it.fqNameForClassNameWithoutDollars.asString()
                }

                return if (containingSymbolOrSelf.isTopLevel) {
                    (firSymbol.fir.getContainingFile()?.psi as? KtFile)
                        ?.takeUnless { it.isScript() }
                        ?.javaFileFacadeFqName?.asString()
                } else {
                    val classId = (containingSymbolOrSelf as? KaConstructorSymbol)?.containingClassId
                        ?: containingSymbolOrSelf.callableId?.classId
                    classId?.takeUnless { it.shortClassName.isSpecial }
                        ?.asFqNameString()
                }
            }
        }
}