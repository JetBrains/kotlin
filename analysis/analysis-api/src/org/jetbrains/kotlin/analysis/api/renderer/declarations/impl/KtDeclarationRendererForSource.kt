/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.impl

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.renderer.base.KaKeywordsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaAnnotationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.KaContextReceiversRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaCallableReturnTypeFilter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaRecommendedRendererCodeStyle
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaRendererTypeApproximator
import org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies.*
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.impl.KaDeclarationModifiersRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.*
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables.*
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers.KaAnonymousObjectSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers.KaNamedClassOrObjectSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers.KaSingleTypeParameterSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers.KaTypeAliasSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes.KaSuperTypeListRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes.KaSuperTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes.KaSuperTypesCallArgumentsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes.KaSuperTypesFilter
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource

@KaExperimentalApi
public object KaDeclarationRendererForSource {
    public val WITH_QUALIFIED_NAMES: KaDeclarationRenderer = KaDeclarationRenderer {
        nameRenderer = KaDeclarationNameRenderer.QUOTED
        keywordsRenderer = KaKeywordsRenderer.AS_WORD
        contextReceiversRenderer = KaContextReceiversRendererForSource.WITH_LABELS
        codeStyle = KaRecommendedRendererCodeStyle
        modifiersRenderer = KaDeclarationModifiersRendererForSource.NO_IMPLICIT_MODIFIERS
        classifierBodyRenderer = KaClassifierBodyRenderer.NO_BODY
        bodyMemberScopeProvider = KaRendererBodyMemberScopeProvider.ALL_DECLARED
        bodyMemberScopeSorter = KaRendererBodyMemberScopeSorter.ENUM_ENTRIES_AT_BEGINING

        superTypeRenderer = KaSuperTypeRenderer.WITH_OUT_APPROXIMATION
        superTypeListRenderer = KaSuperTypeListRenderer.AS_LIST
        superTypesFilter = KaSuperTypesFilter.NO_DEFAULT_TYPES
        superTypesArgumentRenderer = KaSuperTypesCallArgumentsRenderer.EMPTY_PARENS
        functionLikeBodyRenderer = KaFunctionLikeBodyRenderer.NO_BODY
        valueParametersRenderer = KaCallableParameterRenderer.PARAMETERS_IN_PARENS

        typeParametersRenderer = KaTypeParametersRenderer.WITH_BOUNDS_IN_WHERE_CLAUSE
        typeParametersFilter = KaTypeParameterRendererFilter.NO_FOR_CONSTURCTORS

        classInitializerRender = KaClassInitializerRenderer.INIT_BLOCK_WITH_BRACES

        anonymousFunctionRenderer = KaAnonymousFunctionSymbolRenderer.AS_SOURCE
        backingFieldRenderer = KaBackingFieldSymbolRenderer.AS_FIELD_KEYWORD
        constructorRenderer = KaConstructorSymbolRenderer.AS_SOURCE
        enumEntryRenderer = KaEnumEntrySymbolRenderer.AS_SOURCE
        functionSymbolRenderer = KaFunctionSymbolRenderer.AS_SOURCE
        javaFieldRenderer = KaJavaFieldSymbolRenderer.AS_SOURCE
        localVariableRenderer = KaLocalVariableSymbolRenderer.AS_SOURCE
        getterRenderer = KaPropertyGetterSymbolRenderer.AS_SOURCE
        setterRenderer = KaPropertySetterSymbolRenderer.AS_SOURCE
        propertyRenderer = KaKotlinPropertySymbolRenderer.AS_SOURCE
        kotlinPropertyRenderer = KaKotlinPropertySymbolRenderer.AS_SOURCE
        syntheticJavaPropertyRenderer = KaSyntheticJavaPropertySymbolRenderer.AS_SOURCE
        valueParameterRenderer = KaValueParameterSymbolRenderer.AS_SOURCE
        samConstructorRenderer = KaSamConstructorSymbolRenderer.NOT_RENDER

        callableSignatureRenderer = KaCallableSignatureRenderer.FOR_SOURCE
        accessorBodyRenderer = KaPropertyAccessorBodyRenderer.NO_BODY
        parameterDefaultValueRenderer = KaParameterDefaultValueRenderer.NO_DEFAULT_VALUE
        variableInitializerRenderer = KaVariableInitializerRenderer.NO_INITIALIZER

        classOrObjectRenderer = KaNamedClassOrObjectSymbolRenderer.AS_SOURCE
        typeAliasRenderer = KaTypeAliasSymbolRenderer.AS_SOURCE
        anonymousObjectRenderer = KaAnonymousObjectSymbolRenderer.AS_SOURCE
        singleTypeParameterRenderer = KaSingleTypeParameterSymbolRenderer.WITHOUT_BOUNDS
        propertyAccessorsRenderer = KaPropertyAccessorsRenderer.NO_DEFAULT
        destructuringDeclarationRenderer = KaDestructuringDeclarationRenderer.WITH_ENTRIES

        callableReceiverRenderer = KaCallableReceiverRenderer.AS_TYPE_WITH_IN_APPROXIMATION
        returnTypeRenderer = KaCallableReturnTypeRenderer.WITH_OUT_APPROXIMATION

        typeRenderer = KaTypeRendererForSource.WITH_QUALIFIED_NAMES
        annotationRenderer = KaAnnotationRendererForSource.WITH_QUALIFIED_NAMES
        declarationTypeApproximator = KaRendererTypeApproximator.TO_DENOTABLE
        returnTypeFilter = KaCallableReturnTypeFilter.NO_UNIT_FOR_FUNCTIONS

        scriptRenderer = KaScriptSymbolRenderer.AS_SOURCE
        scriptInitializerRenderer = KaScriptInitializerRenderer.NO_INITIALIZER
    }

    public val WITH_SHORT_NAMES: KaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        annotationRenderer = KaAnnotationRendererForSource.WITH_SHORT_NAMES
        typeRenderer = KaTypeRendererForSource.WITH_SHORT_NAMES
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaDeclarationRendererForSource' instead", ReplaceWith("KaDeclarationRendererForSource"))
public typealias KtDeclarationRendererForSource = KaDeclarationRendererForSource