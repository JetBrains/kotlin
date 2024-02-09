/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name

interface DestructuringContext<T> {
    val T.returnTypeRef: FirTypeRef
    val T.name: Name
    val T.source: KtSourceElement
    fun T.extractAnnotationsTo(target: FirAnnotationContainerBuilder, containerSymbol: FirBasedSymbol<*>)
    fun createComponentCall(container: FirVariable, entrySource: KtSourceElement?, index: Int): FirExpression {
        return container.toComponentCall(entrySource, index)
    }
}

context(AbstractRawFirBuilder<*>, DestructuringContext<T>)
fun <T> MutableList<in FirVariable>.addDestructuringVariables(
    moduleData: FirModuleData,
    container: FirVariable,
    entries: List<T>,
    isVar: Boolean,
    tmpVariable: Boolean,
    localEntries: Boolean,
    configure: (FirVariable) -> Unit = {}
) {
    if (tmpVariable) {
        this += container
    }
    for ((index, entry) in entries.withIndex()) {
        this += buildDestructuringVariable(
            moduleData,
            container,
            entry,
            isVar,
            localEntries,
            index,
            configure,
        )
    }
}

context(AbstractRawFirBuilder<*>, DestructuringContext<T>)
fun <T> buildDestructuringVariable(
    moduleData: FirModuleData,
    container: FirVariable,
    entry: T,
    isVar: Boolean,
    localEntries: Boolean,
    index: Int,
    configure: (FirVariable) -> Unit = {}
): FirVariable = buildProperty {
    symbol = FirPropertySymbol(entry.name)
    withContainerSymbol(symbol, localEntries) {
        this.moduleData = moduleData
        origin = FirDeclarationOrigin.Source
        returnTypeRef = entry.returnTypeRef
        name = entry.name
        initializer = createComponentCall(container, entry.source, index)
        this.isVar = isVar
        source = entry.source
        isLocal = localEntries
        status = FirDeclarationStatusImpl(if (localEntries) Visibilities.Local else Visibilities.Public, Modality.FINAL)
        entry.extractAnnotationsTo(this, context.containerSymbol)
    }
}.also(configure)