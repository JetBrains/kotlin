/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.impl

import org.jetbrains.kotlin.analysis.api.renderer.base.KtKeywordRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KtAnnotationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.KtContextReceiversRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtCallableReturnTypeFilter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtRecommendedRendererCodeStyle
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtRendererTypeApproximator
import org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies.*
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.impl.KtDeclarationModifiersRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.*
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables.*
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers.KtAnonymousObjectSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers.KtNamedClassOrObjectSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers.KtSingleTypeParameterSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers.KtTypeAliasSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes.KtSuperTypeListRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes.KtSuperTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes.KtSuperTypesCallArgumentsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes.KtSuperTypesFilter
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KtTypeRendererForSource

public object KtDeclarationRendererForSource {
    public val WITH_QUALIFIED_NAMES: KtDeclarationRenderer = KtDeclarationRenderer {
        nameRenderer = KtDeclarationNameRenderer.QUOTED
        keywordRender = KtKeywordRenderer.AS_WORD
        contextReceiversRenderer = KtContextReceiversRendererForSource.WITH_LABELS
        codeStyle = KtRecommendedRendererCodeStyle
        modifiersRenderer = KtDeclarationModifiersRendererForSource.NO_IMPLICIT_MODIFIERS
        classifierBodyRenderer = KtClassifierBodyRenderer.NO_BODY
        bodyMemberScopeProvider = KtRendererBodyMemberScopeProvider.ALL_DECLARED
        bodyMemberScopeSorter = KtRendererBodyMemberScopeSorter.ENUM_ENTRIES_AT_BEGINING

        superTypeRenderer = KtSuperTypeRenderer.WITH_OUT_APPROXIMATION
        superTypeListRenderer = KtSuperTypeListRenderer.AS_LIST
        superTypesFilter = KtSuperTypesFilter.NO_DEFAULT_TYPES
        superTypesArgumentRenderer = KtSuperTypesCallArgumentsRenderer.EMPTY_PARENS
        functionLikeBodyRenderer = KtFunctionLikeBodyRenderer.NO_BODY
        valueParametersRenderer = KtCallableParameterRenderer.PARAMETERS_IN_PARENS

        typeParametersRenderer = KtTypeParametersRenderer.WITH_BOUNDS_IN_WHERE_CLAUSE
        typeParametersFilter = KtTypeParameterRendererFilter.NO_FOR_CONSTURCTORS

        classInitializerRender = KtClassInitializerRenderer.INIT_BLOCK_WITH_BRACES

        anonymousFunctionRenderer = KtAnonymousFunctionSymbolRenderer.AS_SOURCE
        backingFieldRenderer = KtBackingFieldSymbolRenderer.AS_FIELD_KEYWORD
        constructorRenderer = KtConstructorSymbolRenderer.AS_SOURCE
        enumEntryRenderer = KtEnumEntrySymbolRenderer.AS_SOURCE
        functionSymbolRenderer = KtFunctionSymbolRenderer.AS_SOURCE
        javaFieldRenderer = KtJavaFieldSymbolRenderer.AS_SOURCE
        localVariableRenderer = KtLocalVariableSymbolRenderer.AS_SOURCE
        getterRenderer = KtPropertyGetterSymbolRenderer.AS_SOURCE
        setterRenderer = KtPropertySetterSymbolRenderer.AS_SOURCE
        propertyRenderer = KtKotlinPropertySymbolRenderer.AS_SOURCE
        kotlinPropertyRenderer = KtKotlinPropertySymbolRenderer.AS_SOURCE
        syntheticJavaPropertyRenderer = KtSyntheticJavaPropertySymbolRenderer.AS_SOURCE
        valueParameterRenderer = KtValueParameterSymbolRenderer.AS_SOURCE
        samConstructorRenderer = KtSamConstructorSymbolRenderer.NOT_RENDER

        callableSignatureRenderer = KtCallableSignatureRenderer.FOR_SOURCE
        accessorBodyRenderer = KtPropertyAccessorBodyRenderer.NO_BODY
        parameterDefaultValueRenderer = KtParameterDefaultValueRenderer.NO_DEFAULT_VALUE
        variableInitializerRenderer = KtVariableInitializerRenderer.NO_INITIALIZER

        classOrObjectRenderer = KtNamedClassOrObjectSymbolRenderer.AS_SOURCE
        typeAliasRenderer = KtTypeAliasSymbolRenderer.AS_SOURCE
        anonymousObjectRenderer = KtAnonymousObjectSymbolRenderer.AS_SOURCE
        singleTypeParameterRenderer = KtSingleTypeParameterSymbolRenderer.WITHOUT_BOUNDS
        propertyAccessorsRenderer = KtPropertyAccessorsRenderer.NO_DEFAULT

        callableReceiverRenderer = KtCallableReceiverRenderer.AS_TYPE_WITH_IN_APPROXIMATION
        returnTypeRenderer = KtCallableReturnTypeRenderer.WITH_OUT_APPROXIMATION

        typeRenderer = KtTypeRendererForSource.WITH_QUALIFIED_NAMES
        annotationRenderer = KtAnnotationRendererForSource.WITH_QUALIFIED_NAMES
        declarationTypeApproximator = KtRendererTypeApproximator.TO_DENOTABLE
        returnTypeFilter = KtCallableReturnTypeFilter.NO_UNIT_FOR_FUNCTIONS

        scriptRenderer = KtScriptSymbolRenderer.AS_SOURCE
        scriptInitializerRenderer = KtScriptInitializerRenderer.NO_INITIALIZER
    }

    public val WITH_SHORT_NAMES: KtDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        annotationRenderer = KtAnnotationRendererForSource.WITH_SHORT_NAMES
        typeRenderer = KtTypeRendererForSource.WITH_SHORT_NAMES
    }
}
