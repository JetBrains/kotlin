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
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildVariableInConditionalExpression
import org.jetbrains.kotlin.fir.generateTemporaryVariable
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
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

fun <T> AbstractRawFirBuilder<*>.addDestructuringVariables(
    destination: MutableList<in FirVariable>,
    c: DestructuringContext<T>,
    moduleData: FirModuleData,
    container: FirVariable,
    entries: List<T>,
    isVar: Boolean,
    tmpVariable: Boolean,
    forceLocal: Boolean,
    configure: (FirVariable) -> Unit = {}
) {
    if (tmpVariable) {
        destination += container
    }
    for ((index, entry) in entries.withIndex()) {
        destination += buildDestructuringVariable(
            moduleData,
            c,
            container,
            entry,
            isVar,
            forceLocal,
            index,
            configure,
        )
    }
}

fun <T> AbstractRawFirBuilder<*>.buildDestructuringVariable(
    moduleData: FirModuleData,
    c: DestructuringContext<T>,
    container: FirVariable,
    entry: T,
    isVar: Boolean,
    forceLocal: Boolean,
    index: Int,
    configure: (FirVariable) -> Unit = {}
): FirVariable = with(c) {
    buildProperty {
        symbol = if (forceLocal) FirPropertySymbol(entry.name) else FirPropertySymbol(callableIdForName(entry.name))
        val localEntries = forceLocal || context.inLocalContext
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

fun <T> AbstractRawFirBuilder<*>.buildIsDestructuring(
    moduleData: FirModuleData,
    originalSource: KtSourceElement?,
    c: DestructuringContext<T>,
    leftHandSide: FirExpression,
    destructuringEntries: List<T>,
    buildTypeOperator: (FirExpression) -> FirTypeOperatorCall
): FirExpression {
    val baseVariable = generateTemporaryVariable(
        moduleData,
        originalSource,
        Name.special("<isDestructor>"),
        leftHandSide
    )
    val wrappedBaseVariable =
        buildVariableInConditionalExpression {
            declaration = baseVariable
            source = baseVariable.source
        }
    val variableAccess = buildPropertyAccessExpression {
        calleeReference = buildResolvedNamedReference {
            name = baseVariable.name
            resolvedSymbol = baseVariable.symbol
        }
        source = originalSource
    }
    val typeOperator = buildTypeOperator(variableAccess)
    val block = buildBlock {
        addDestructuringVariables(
            statements,
            c,
            baseModuleData,
            baseVariable,
            destructuringEntries,
            false,
            false,
            false
        )
    }
    val code: List<FirExpression> =
        listOf(wrappedBaseVariable, typeOperator) + block.statements.filterIsInstance<FirVariable>().map<_, FirExpression> {
            buildVariableInConditionalExpression {
                declaration = it
                source = it.source
            }
        }
    return code.reduce { acc, next ->
        acc.generateLazyLogicalOperation(
            next,
            isAnd = true,
            originalSource
        )
    }
}
