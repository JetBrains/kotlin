/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.buildErrorExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

interface DestructuringContext<T> {
    val T.returnTypeRef: FirTypeRef
    val T.name: Name
    val T.initializerName: Name?
    val T.isVar: Boolean
    val T.source: KtSourceElement
    val T.initializerSource: KtSourceElement?
    fun T.extractAnnotationsTo(target: FirAnnotationContainerBuilder, containerSymbol: FirBasedSymbol<*>)

    fun interceptExpressionBuilding(
        sourceElement: KtSourceElement?,
        buildExpression: () -> FirExpression,
    ): FirExpression = buildExpression()
}

context(c: DestructuringContext<T>)
fun <T> AbstractRawFirBuilder<*>.addDestructuringVariables(
    destination: MutableList<in FirVariable>,
    moduleData: FirModuleData,
    container: FirVariable,
    entries: List<T>,
    isNameBased: Boolean,
    isTmpVariable: Boolean,
    forceLocal: Boolean,
    configure: (FirVariable) -> Unit = {}
) {
    if (isTmpVariable) {
        destination += container
    }
    for ((index, entry) in entries.withIndex()) {
        destination += buildDestructuringVariable(
            moduleData,
            container,
            entry,
            isNameBased,
            forceLocal,
            index,
            configure,
        )
    }
}

context(c: DestructuringContext<T>)
fun <T> AbstractRawFirBuilder<*>.buildDestructuringVariable(
    moduleData: FirModuleData,
    container: FirVariable,
    entry: T,
    isNameBased: Boolean,
    forceLocal: Boolean,
    index: Int,
    configure: (FirVariable) -> Unit = {}
): FirVariable = with(c) {
    buildProperty {
        val localEntries = forceLocal || context.inLocalContext
        symbol = when {
            localEntries -> FirLocalPropertySymbol()
            else -> FirRegularPropertySymbol(callableIdForName(entry.name))
        }
        withContainerSymbol(symbol, localEntries) {
            this.moduleData = moduleData
            origin = FirDeclarationOrigin.Source
            returnTypeRef = entry.returnTypeRef
            name = entry.name
            initializer = interceptExpressionBuilding(entry.source) {
                if (isNameBased) {
                    val entryFakeSource = entry.source.fakeElement(KtFakeSourceElementKind.DesugaredNameBasedDestructuring)
                    val initializerFakeSource = entry.initializerSource?.fakeElement(KtFakeSourceElementKind.DesugaredNameBasedDestructuring) ?: entryFakeSource

                    if (entry.name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR && entry.initializerName == null) {
                        buildErrorExpression(
                            initializerFakeSource,
                            ConeSimpleDiagnostic(
                                "Underscore without renaming in destructuring",
                                DiagnosticKind.UnderscoreWithoutRenamingInDestructuring
                            )
                        )
                    } else {
                        buildPropertyAccessExpression {
                            source = initializerFakeSource
                            explicitReceiver = generateResolvedAccessExpression(entryFakeSource, container)
                            calleeReference = buildSimpleNamedReference {
                                this.source = initializerFakeSource
                                name = entry.initializerName ?: entry.name
                            }
                        }
                    }
                } else {
                    container.toComponentCall(entry.source, index)
                }
            }
            this.isVar = entry.isVar
            source = entry.source
            status = FirDeclarationStatusImpl(if (localEntries) Visibilities.Local else Visibilities.Public, Modality.FINAL)
            isLocal = localEntries
            entry.extractAnnotationsTo(this, context.containerSymbol)
            if (!localEntries) {
                getter = FirDefaultPropertyGetter(
                    source = source?.fakeElement(KtFakeSourceElementKind.DefaultAccessor),
                    moduleData = moduleData,
                    origin = FirDeclarationOrigin.Source,
                    propertyTypeRef = returnTypeRef,
                    visibility = Visibilities.Public,
                    propertySymbol = symbol,
                    modality = Modality.FINAL,
                )
                if (entry.isVar) {
                    setter = FirDefaultPropertySetter(
                        source = source?.fakeElement(KtFakeSourceElementKind.DefaultAccessor),
                        moduleData = moduleData,
                        origin = FirDeclarationOrigin.Source,
                        propertyTypeRef = returnTypeRef,
                        visibility = Visibilities.Public,
                        propertySymbol = symbol,
                        modality = Modality.FINAL,
                    )
                }
            }
        }
    }.also(configure)
}
