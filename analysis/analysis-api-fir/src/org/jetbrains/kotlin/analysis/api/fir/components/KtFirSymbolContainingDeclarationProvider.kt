/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaSymbolContainingDeclarationProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.jvmClassNameIfDeserialized
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.psi.*

internal class KaFirSymbolContainingDeclarationProvider(
    override val analysisSession: KaFirSession,
    override val token: KaLifetimeToken,
) : KaSymbolContainingDeclarationProvider(), KaFirSessionComponent {
    override fun getContainingJvmClassName(symbol: KaCallableSymbol): String? = with(analysisSession) {
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
