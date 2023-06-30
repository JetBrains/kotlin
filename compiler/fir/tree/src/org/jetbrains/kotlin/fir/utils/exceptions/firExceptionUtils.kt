/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.utils.exceptions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.renderer.*
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder

fun ExceptionAttachmentBuilder.withFirEntry(name: String, fir: FirElement) {
    withEntry(name, fir) {
        FirRenderer(
            resolvePhaseRenderer = FirResolvePhaseRenderer(),
            declarationRenderer = FirDeclarationRendererWithAttributes(),
            fileAnnotationsContainerRenderer = FirFileAnnotationsContainerRenderer(),
        ).renderElementAsString(it)
    }
    withEntry("${name}ElementKind", fir.source?.kind?.let { it::class.simpleName })
    if (fir is FirElementWithResolveState) {
        withModuleDataEntry("${name}ModuleData", fir.moduleData)
    }
    withSourceEntry("${name}Source", fir.source)
}

fun ExceptionAttachmentBuilder.withFirSymbolIdEntry(name: String, symbol: FirBasedSymbol<*>?) {
    when (symbol) {
        is FirClassifierSymbol -> withFirLookupTagEntry(name, symbol.toLookupTag())
        is FirCallableSymbol -> withEntry(name, symbol.callableId.toString())
        else -> withEntry(name, symbol.toString())
    }
}

fun ExceptionAttachmentBuilder.withFirLookupTagEntry(name: String, lookupTag: ConeClassifierLookupTag?) {
    withEntry(name, lookupTag) { tag ->
        when (tag) {
            is ConeClassLikeLookupTag -> tag.classId.asString()
            else -> tag.name.asString()
        }
    }
}

fun ExceptionAttachmentBuilder.withSourceEntry(name: String, source: KtSourceElement?) {
    withEntry(name, source) { it.getElementTextInContextForDebug() }
}

fun ExceptionAttachmentBuilder.withModuleDataEntry(name: String, moduleData: FirModuleData?) {
    withEntry(name, moduleData) { module ->
        buildString {
            append("Name: ${module.name}, ")
            append("Platform: ${module.platform}")
        }
    }
}

fun ExceptionAttachmentBuilder.withFirSymbolEntry(name: String, symbol: FirBasedSymbol<*>) {
    withFirEntry("${name}Fir", symbol.fir)
}

fun ExceptionAttachmentBuilder.withConeTypeEntry(name: String, coneType: ConeKotlinType?) {
    withEntry(name, coneType) {
        buildString { ConeTypeRendererForDebugging(this).render(it) }
    }
}