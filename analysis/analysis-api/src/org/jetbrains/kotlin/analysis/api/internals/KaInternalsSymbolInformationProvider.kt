/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaExperimentalApi::class)

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationTarget
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.FqName

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsSymbolInformationProvider {
    public fun deprecation(symbol: KaSymbol): KaDeprecation?

    public fun isDeprecated(symbol: KaSymbol): Boolean

    public fun canBeOperator(symbol: KaNamedFunctionSymbol): Boolean

    public fun applicableAnnotationTargets(symbol: KaClassSymbol): Set<KaAnnotationTarget>?

    public fun isInline(symbol: KaKotlinPropertySymbol): Boolean

    public fun importableFqName(symbol: KaSymbol): FqName?

    public fun defaultAnnotationTargets(symbol: KaSymbol): Set<KaAnnotationTarget>?

    public fun returnValueStatus(symbol: KaNamedFunctionSymbol): KaReturnValueStatus

    public fun containingFileAnnotations(symbol: KaDeclarationSymbol): KaAnnotationList?
}
