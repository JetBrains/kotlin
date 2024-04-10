/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.base.KtKeywordsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KtAnnotationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.KtContextReceiversRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies.*
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.KtDeclarationModifiersRenderer
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
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public class KtDeclarationRenderer private constructor(
    public val nameRenderer: KtDeclarationNameRenderer,
    public val keywordsRenderer: KtKeywordsRenderer,
    public val contextReceiversRenderer: KtContextReceiversRenderer,
    public val codeStyle: KtRendererCodeStyle,
    public val typeRenderer: KtTypeRenderer,
    public val annotationRenderer: KtAnnotationRenderer,
    public val modifiersRenderer: KtDeclarationModifiersRenderer,
    public val declarationTypeApproximator: KtRendererTypeApproximator,
    public val classifierBodyRenderer: KtClassifierBodyRenderer,


    public val superTypeRenderer: KtSuperTypeRenderer,
    public val superTypeListRenderer: KtSuperTypeListRenderer,
    public val superTypesFilter: KtSuperTypesFilter,
    public val superTypesArgumentRenderer: KtSuperTypesCallArgumentsRenderer,

    public val bodyMemberScopeProvider: KtRendererBodyMemberScopeProvider,
    public val bodyMemberScopeSorter: KtRendererBodyMemberScopeSorter,

    public val functionLikeBodyRenderer: KtFunctionLikeBodyRenderer,
    public val variableInitializerRenderer: KtVariableInitializerRenderer,
    public val parameterDefaultValueRenderer: KtParameterDefaultValueRenderer,
    public val accessorBodyRenderer: KtPropertyAccessorBodyRenderer,

    public val returnTypeRenderer: KtCallableReturnTypeRenderer,
    public val callableReceiverRenderer: KtCallableReceiverRenderer,

    public val valueParametersRenderer: KtCallableParameterRenderer,
    public val typeParametersRenderer: KtTypeParametersRenderer,
    public val typeParametersFilter: KtTypeParameterRendererFilter,

    public val callableSignatureRenderer: KtCallableSignatureRenderer,

    public val anonymousFunctionRenderer: KtAnonymousFunctionSymbolRenderer,
    public val backingFieldRenderer: KtBackingFieldSymbolRenderer,
    public val constructorRenderer: KtConstructorSymbolRenderer,
    public val enumEntryRenderer: KtEnumEntrySymbolRenderer,
    public val functionSymbolRenderer: KtFunctionSymbolRenderer,
    public val javaFieldRenderer: KtJavaFieldSymbolRenderer,
    public val localVariableRenderer: KtLocalVariableSymbolRenderer,
    public val getterRenderer: KtPropertyGetterSymbolRenderer,
    public val setterRenderer: KtPropertySetterSymbolRenderer,
    public val propertyRenderer: KtKotlinPropertySymbolRenderer,
    public val kotlinPropertyRenderer: KtKotlinPropertySymbolRenderer,
    public val syntheticJavaPropertyRenderer: KtSyntheticJavaPropertySymbolRenderer,
    public val valueParameterRenderer: KtValueParameterSymbolRenderer,
    public val samConstructorRenderer: KtSamConstructorSymbolRenderer,
    public val propertyAccessorsRenderer: KtPropertyAccessorsRenderer,
    public val destructuringDeclarationRenderer: KtDestructuringDeclarationRenderer,

    public val classInitializerRender: KtClassInitializerRenderer,
    public val classOrObjectRenderer: KtNamedClassOrObjectSymbolRenderer,
    public val typeAliasRenderer: KtTypeAliasSymbolRenderer,
    public val anonymousObjectRenderer: KtAnonymousObjectSymbolRenderer,
    public val singleTypeParameterRenderer: KtSingleTypeParameterSymbolRenderer,
    public val returnTypeFilter: KtCallableReturnTypeFilter,

    public val scriptRenderer: KtScriptSymbolRenderer,
    public val scriptInitializerRenderer: KtScriptInitializerRenderer
) {

    public fun renderDeclaration(analysisSession: KtAnalysisSession, symbol: KtDeclarationSymbol, printer: PrettyPrinter) {
        when (symbol) {
            is KtAnonymousObjectSymbol -> anonymousObjectRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtNamedClassOrObjectSymbol -> classOrObjectRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtTypeAliasSymbol -> typeAliasRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtAnonymousFunctionSymbol -> anonymousFunctionRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtConstructorSymbol -> constructorRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtFunctionSymbol -> functionSymbolRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtPropertyGetterSymbol -> getterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtPropertySetterSymbol -> setterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtSamConstructorSymbol -> samConstructorRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtBackingFieldSymbol -> backingFieldRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtEnumEntrySymbol -> enumEntryRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtValueParameterSymbol -> valueParameterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtJavaFieldSymbol -> javaFieldRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtLocalVariableSymbol -> localVariableRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtKotlinPropertySymbol -> kotlinPropertyRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtSyntheticJavaPropertySymbol -> syntheticJavaPropertyRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtTypeParameterSymbol -> singleTypeParameterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtClassInitializerSymbol -> classInitializerRender.renderClassInitializer(analysisSession, symbol, this, printer)
            is KtScriptSymbol -> scriptRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KtDestructuringDeclarationSymbol -> destructuringDeclarationRenderer.renderSymbol(analysisSession, symbol, this, printer)
        }
    }

    public fun with(action: Builder.() -> Unit): KtDeclarationRenderer {
        val renderer = this
        return KtDeclarationRenderer {
            this.nameRenderer = renderer.nameRenderer
            this.keywordsRenderer = renderer.keywordsRenderer
            this.contextReceiversRenderer = renderer.contextReceiversRenderer
            this.codeStyle = renderer.codeStyle
            this.typeRenderer = renderer.typeRenderer
            this.annotationRenderer = renderer.annotationRenderer
            this.modifiersRenderer = renderer.modifiersRenderer
            this.declarationTypeApproximator = renderer.declarationTypeApproximator
            this.classifierBodyRenderer = renderer.classifierBodyRenderer

            this.superTypeRenderer = renderer.superTypeRenderer
            this.superTypeListRenderer = renderer.superTypeListRenderer
            this.superTypesFilter = renderer.superTypesFilter
            this.superTypesArgumentRenderer = renderer.superTypesArgumentRenderer

            this.bodyMemberScopeProvider = renderer.bodyMemberScopeProvider
            this.bodyMemberScopeSorter = renderer.bodyMemberScopeSorter

            this.functionLikeBodyRenderer = renderer.functionLikeBodyRenderer
            this.variableInitializerRenderer = renderer.variableInitializerRenderer
            this.parameterDefaultValueRenderer = renderer.parameterDefaultValueRenderer
            this.accessorBodyRenderer = renderer.accessorBodyRenderer

            this.returnTypeRenderer = renderer.returnTypeRenderer
            this.callableReceiverRenderer = renderer.callableReceiverRenderer

            this.valueParametersRenderer = renderer.valueParametersRenderer
            this.typeParametersRenderer = renderer.typeParametersRenderer
            this.typeParametersFilter = renderer.typeParametersFilter

            this.callableSignatureRenderer = renderer.callableSignatureRenderer

            this.anonymousFunctionRenderer = renderer.anonymousFunctionRenderer
            this.backingFieldRenderer = renderer.backingFieldRenderer
            this.constructorRenderer = renderer.constructorRenderer
            this.enumEntryRenderer = renderer.enumEntryRenderer
            this.functionSymbolRenderer = renderer.functionSymbolRenderer
            this.javaFieldRenderer = renderer.javaFieldRenderer
            this.localVariableRenderer = renderer.localVariableRenderer
            this.getterRenderer = renderer.getterRenderer
            this.setterRenderer = renderer.setterRenderer
            this.propertyRenderer = renderer.propertyRenderer
            this.kotlinPropertyRenderer = renderer.kotlinPropertyRenderer
            this.syntheticJavaPropertyRenderer = renderer.syntheticJavaPropertyRenderer
            this.valueParameterRenderer = renderer.valueParameterRenderer
            this.samConstructorRenderer = renderer.samConstructorRenderer
            this.propertyAccessorsRenderer = renderer.propertyAccessorsRenderer
            this.destructuringDeclarationRenderer = renderer.destructuringDeclarationRenderer

            this.classInitializerRender = renderer.classInitializerRender
            this.classOrObjectRenderer = renderer.classOrObjectRenderer
            this.typeAliasRenderer = renderer.typeAliasRenderer
            this.anonymousObjectRenderer = renderer.anonymousObjectRenderer
            this.singleTypeParameterRenderer = renderer.singleTypeParameterRenderer
            this.returnTypeFilter = renderer.returnTypeFilter

            this.scriptRenderer = renderer.scriptRenderer
            this.scriptInitializerRenderer = renderer.scriptInitializerRenderer

            action()
        }
    }

    public companion object {
        public operator fun invoke(action: Builder.() -> Unit): KtDeclarationRenderer =
            Builder().apply(action).build()
    }

    public open class Builder {
        public lateinit var returnTypeFilter: KtCallableReturnTypeFilter
        public lateinit var nameRenderer: KtDeclarationNameRenderer
        public lateinit var contextReceiversRenderer: KtContextReceiversRenderer
        public lateinit var keywordsRenderer: KtKeywordsRenderer
        public lateinit var codeStyle: KtRendererCodeStyle
        public lateinit var typeRenderer: KtTypeRenderer
        public lateinit var annotationRenderer: KtAnnotationRenderer
        public lateinit var modifiersRenderer: KtDeclarationModifiersRenderer
        public lateinit var declarationTypeApproximator: KtRendererTypeApproximator
        public lateinit var classifierBodyRenderer: KtClassifierBodyRenderer

        public lateinit var superTypeRenderer: KtSuperTypeRenderer
        public lateinit var superTypeListRenderer: KtSuperTypeListRenderer
        public lateinit var superTypesFilter: KtSuperTypesFilter
        public lateinit var superTypesArgumentRenderer: KtSuperTypesCallArgumentsRenderer

        public lateinit var bodyMemberScopeProvider: KtRendererBodyMemberScopeProvider
        public lateinit var bodyMemberScopeSorter: KtRendererBodyMemberScopeSorter

        public lateinit var functionLikeBodyRenderer: KtFunctionLikeBodyRenderer
        public lateinit var variableInitializerRenderer: KtVariableInitializerRenderer
        public lateinit var parameterDefaultValueRenderer: KtParameterDefaultValueRenderer
        public lateinit var accessorBodyRenderer: KtPropertyAccessorBodyRenderer

        public lateinit var returnTypeRenderer: KtCallableReturnTypeRenderer
        public lateinit var callableReceiverRenderer: KtCallableReceiverRenderer

        public lateinit var valueParametersRenderer: KtCallableParameterRenderer
        public lateinit var typeParametersRenderer: KtTypeParametersRenderer
        public lateinit var typeParametersFilter: KtTypeParameterRendererFilter
        public lateinit var callableSignatureRenderer: KtCallableSignatureRenderer

        public lateinit var anonymousFunctionRenderer: KtAnonymousFunctionSymbolRenderer
        public lateinit var backingFieldRenderer: KtBackingFieldSymbolRenderer
        public lateinit var constructorRenderer: KtConstructorSymbolRenderer
        public lateinit var enumEntryRenderer: KtEnumEntrySymbolRenderer
        public lateinit var functionSymbolRenderer: KtFunctionSymbolRenderer
        public lateinit var javaFieldRenderer: KtJavaFieldSymbolRenderer
        public lateinit var localVariableRenderer: KtLocalVariableSymbolRenderer
        public lateinit var getterRenderer: KtPropertyGetterSymbolRenderer
        public lateinit var setterRenderer: KtPropertySetterSymbolRenderer
        public lateinit var propertyRenderer: KtKotlinPropertySymbolRenderer
        public lateinit var kotlinPropertyRenderer: KtKotlinPropertySymbolRenderer
        public lateinit var syntheticJavaPropertyRenderer: KtSyntheticJavaPropertySymbolRenderer
        public lateinit var valueParameterRenderer: KtValueParameterSymbolRenderer
        public lateinit var samConstructorRenderer: KtSamConstructorSymbolRenderer
        public lateinit var propertyAccessorsRenderer: KtPropertyAccessorsRenderer
        public lateinit var destructuringDeclarationRenderer: KtDestructuringDeclarationRenderer

        public lateinit var classInitializerRender: KtClassInitializerRenderer
        public lateinit var classOrObjectRenderer: KtNamedClassOrObjectSymbolRenderer
        public lateinit var typeAliasRenderer: KtTypeAliasSymbolRenderer
        public lateinit var anonymousObjectRenderer: KtAnonymousObjectSymbolRenderer
        public lateinit var singleTypeParameterRenderer: KtSingleTypeParameterSymbolRenderer

        public lateinit var scriptRenderer: KtScriptSymbolRenderer
        public lateinit var scriptInitializerRenderer: KtScriptInitializerRenderer

        public fun build(): KtDeclarationRenderer = KtDeclarationRenderer(
            nameRenderer,
            keywordsRenderer,
            contextReceiversRenderer,
            codeStyle,
            typeRenderer,
            annotationRenderer,
            modifiersRenderer,
            declarationTypeApproximator,
            classifierBodyRenderer,

            superTypeRenderer,
            superTypeListRenderer,
            superTypesFilter,
            superTypesArgumentRenderer,

            bodyMemberScopeProvider,
            bodyMemberScopeSorter,

            functionLikeBodyRenderer,
            variableInitializerRenderer,
            parameterDefaultValueRenderer,
            accessorBodyRenderer,

            returnTypeRenderer,
            callableReceiverRenderer,

            valueParametersRenderer,
            typeParametersRenderer,
            typeParametersFilter,
            callableSignatureRenderer,

            anonymousFunctionRenderer,
            backingFieldRenderer,
            constructorRenderer,
            enumEntryRenderer,
            functionSymbolRenderer,
            javaFieldRenderer,
            localVariableRenderer,
            getterRenderer,
            setterRenderer,
            propertyRenderer,
            kotlinPropertyRenderer,
            syntheticJavaPropertyRenderer,
            valueParameterRenderer,
            samConstructorRenderer,
            propertyAccessorsRenderer,
            destructuringDeclarationRenderer,

            classInitializerRender,
            classOrObjectRenderer,
            typeAliasRenderer,
            anonymousObjectRenderer,
            singleTypeParameterRenderer,
            returnTypeFilter,

            scriptRenderer,
            scriptInitializerRenderer,
        )
    }
}

