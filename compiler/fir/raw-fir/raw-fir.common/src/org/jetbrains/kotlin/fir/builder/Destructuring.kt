/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
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

/**
 * @param isMember whether the entries are members of a class or a file even in a local context, e.g., in a local class.
 *   It is the case for top-level and member destructuring declarations outside of scripts.
 * @param isCompanionBlockMember whether the entries are static members declared in a companion block.
 */
context(c: DestructuringContext<T>)
fun <T> AbstractRawFirBuilder<*, *>.addDestructuringVariables(
    destination: MutableList<in FirVariable>,
    context: Context<*>,
    moduleData: FirModuleData,
    container: FirVariable,
    entries: List<T>,
    kind: DestructuringKind,
    isTmpVariable: Boolean,
    forceLocal: Boolean,
    isMember: Boolean = false,
    isCompanionBlockMember: Boolean = false,
    configure: (FirVariable) -> Unit = {}
) {
    if (isTmpVariable) {
        destination += container
    }
    for ([index, entry] in entries.withIndex()) {
        destination += buildDestructuringVariable(
            context,
            moduleData,
            container,
            entry,
            kind,
            forceLocal,
            index,
            isMember,
            isCompanionBlockMember,
            configure,
        )
    }
}

enum class DestructuringKind {
    NameBased,
    PositionalWithSquareBrackets,
    PositionalWithParentheses,
}

context(c: DestructuringContext<T>)
fun <T> AbstractRawFirBuilder<*, *>.buildDestructuringVariable(
    context: Context<*>,
    moduleData: FirModuleData,
    container: FirVariable,
    entry: T,
    kind: DestructuringKind,
    forceLocal: Boolean,
    index: Int,
    isMember: Boolean = false,
    isCompanionBlockMember: Boolean = false,
    configure: (FirVariable) -> Unit = {}
): FirVariable = with(c) {
    buildProperty {
        val inLocalContext = forceLocal || context.inLocalContext
        val localEntries = inLocalContext && !isMember
        symbol = when {
            localEntries -> FirLocalPropertySymbol()
            else -> FirRegularPropertySymbol(callableIdForName(entry.name))
        }
        context.withContainerSymbol(symbol, localEntries) {
            this.moduleData = moduleData
            origin = FirDeclarationOrigin.Source
            returnTypeRef = entry.returnTypeRef
            name = entry.name
            initializer = interceptExpressionBuilding(entry.source) {
                if (kind == DestructuringKind.NameBased) {
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
                    container.toComponentCall(
                        entrySource = entry.source,
                        index = index,
                        initializerName = entry.initializerName ?: entry.name,
                        isShortFormWithParentheses = kind == DestructuringKind.PositionalWithParentheses
                    )
                }
            }
            this.isVar = entry.isVar
            source = entry.source
            status = FirDeclarationStatusImpl(if (localEntries) Visibilities.Local else Visibilities.Public, Modality.FINAL).apply {
                isStatic = isCompanionBlockMember
            }

            isLocal = inLocalContext
            context.containerSymbolIfAny?.let { entry.extractAnnotationsTo(this, it) }
            if (!localEntries) {
                if (!isCompanionBlockMember) {
                    dispatchReceiverType = currentDispatchReceiverType(context)
                }

                getter = FirDefaultPropertyGetter(
                    source = source?.fakeElement(KtFakeSourceElementKind.DefaultAccessor.Getter),
                    moduleData = moduleData,
                    origin = FirDeclarationOrigin.Source,
                    propertyTypeRef = returnTypeRef,
                    propertySymbol = symbol,
                    status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL).apply { isStatic = isCompanionBlockMember },
                )
                if (entry.isVar) {
                    setter = FirDefaultPropertySetter(
                        source = source?.fakeElement(KtFakeSourceElementKind.DefaultAccessor.Setter),
                        moduleData = moduleData,
                        origin = FirDeclarationOrigin.Source,
                        propertyTypeRef = returnTypeRef,
                        propertySymbol = symbol,
                        status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL).apply { isStatic = isCompanionBlockMember },
                    )
                }
            }
        }
    }.also {
        if (isCompanionBlockMember) {
            it.initContainingClassAttr(context)
            it.getter?.initContainingClassAttr(context)
            it.setter?.initContainingClassAttr(context)
        }

        configure(it)
    }
}
