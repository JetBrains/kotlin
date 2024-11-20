/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.Name

interface NameBasedDestructuringContext<T> : DestructuringContext<T> {
    val T.propertyAccessorName: Name
    val T.isVar: Boolean
    fun createAssigmentCall(container: FirVariable, propertyName: Name, entrySource: KtSourceElement?): FirExpression {
        val propertyAccessExpression = buildPropertyAccessExpression {
            source = entrySource
            explicitReceiver = generateResolvedAccessExpression(source, container)
            this.calleeReference = buildSimpleNamedReference {
                source = entrySource
                name = propertyName
            }
        }
        return propertyAccessExpression
    }
}

fun <T> AbstractRawFirBuilder<*>.addNameBasedDestructuringVariables(
    destination: MutableList<in FirVariable>,
    c: NameBasedDestructuringContext<T>,
    moduleData: FirModuleData,
    container: FirVariable,
    entries: List<T>,
    tmpVariable: Boolean,
    forceLocal: Boolean,
    configure: (FirVariable) -> Unit = {},
) {
    if (tmpVariable) {
        destination += container
    }
    for (entry in entries) {
        destination += buildNameBasedDestructuringVariable(
            moduleData,
            c,
            container,
            entry,
            forceLocal,
            configure,
        )
    }
}

fun <T> AbstractRawFirBuilder<*>.buildNameBasedDestructuringVariable(
    moduleData: FirModuleData,
    c: NameBasedDestructuringContext<T>,
    container: FirVariable,
    entry: T,
    forceLocal: Boolean,
    configure: (FirVariable) -> Unit = {},
): FirVariable = with(c) {
    buildProperty {
        symbol = if (forceLocal) FirPropertySymbol(entry.name) else FirPropertySymbol(callableIdForName(entry.name))
        val localEntries = forceLocal || context.inLocalContext
        withContainerSymbol(symbol, localEntries) {
            this.moduleData = moduleData
            origin = FirDeclarationOrigin.Source
            returnTypeRef = entry.returnTypeRef
            name = entry.name
            initializer = createAssigmentCall(container, entry.propertyAccessorName, entry.source)
            this.isVar = entry.isVar
            source = entry.source
            isLocal = localEntries
            status = FirDeclarationStatusImpl(if (localEntries) Visibilities.Local else Visibilities.Public, Modality.FINAL)
            if (!localEntries) {
                getter = FirDefaultPropertyGetter(
                    source?.fakeElement(KtFakeSourceElementKind.DefaultAccessor), moduleData,
                    FirDeclarationOrigin.Source, returnTypeRef, Visibilities.Public, symbol,
                )
                if (isVar) {
                    setter = FirDefaultPropertySetter(
                        source?.fakeElement(KtFakeSourceElementKind.DefaultAccessor), moduleData,
                        FirDeclarationOrigin.Source, returnTypeRef, Visibilities.Public, symbol,
                    )
                }
            }
        }
    }.also(configure)
}
