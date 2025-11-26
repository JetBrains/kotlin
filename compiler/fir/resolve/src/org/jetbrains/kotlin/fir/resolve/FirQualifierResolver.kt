/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.types.FirQualifierPart
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class FirQualifierResolver : FirSessionComponent {
    abstract fun resolveSymbolWithPrefix(
        prefix: ClassId,
        remainingParts: List<FirQualifierPart>
    ): FirClassifierSymbol<*>?

    abstract fun resolveFullyQualifiedSymbol(
        parts: List<FirQualifierPart>,
        packageImports: Map<Name, List<FqName>>
    ): Pair<FirClassifierSymbol<*>, FirResolvedSymbolOrigin>?

    companion object {
        context(_: SessionHolder)
        fun isRootIdePackageAllowed(): Boolean = AnalysisFlags.ideMode.isSet() || LanguageFeature.ForbidRootIdePackageInCli.isDisabled()

        context(_: SessionHolder)
        fun isRootIdePackageDeprecated(): Boolean = !AnalysisFlags.ideMode.isSet() && LanguageFeature.ForbidRootIdePackageInCli.isDisabled()
    }
}

val FirSession.qualifierResolver: FirQualifierResolver by FirSession.sessionComponentAccessor()
