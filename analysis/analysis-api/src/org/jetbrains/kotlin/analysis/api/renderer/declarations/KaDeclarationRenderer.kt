/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.base.KaKeywordsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaAnnotationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.KaContextReceiversRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies.*
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.KaDeclarationModifiersRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.*
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables.*
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers.KaAnonymousObjectSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers.KaNamedClassSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers.KaSingleTypeParameterSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers.KaTypeAliasSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes.KaSuperTypeListRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes.KaSuperTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes.KaSuperTypesCallArgumentsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes.KaSuperTypesFilter
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
public class KaDeclarationRenderer private constructor(
    public val nameRenderer: KaDeclarationNameRenderer,
    public val keywordsRenderer: KaKeywordsRenderer,
    public val contextReceiversRenderer: KaContextReceiversRenderer,
    public val codeStyle: KaRendererCodeStyle,
    public val typeRenderer: KaTypeRenderer,
    public val annotationRenderer: KaAnnotationRenderer,
    public val modifiersRenderer: KaDeclarationModifiersRenderer,
    public val declarationTypeApproximator: KaRendererTypeApproximator,
    public val classifierBodyRenderer: KaClassifierBodyRenderer,


    public val superTypeRenderer: KaSuperTypeRenderer,
    public val superTypeListRenderer: KaSuperTypeListRenderer,
    public val superTypesFilter: KaSuperTypesFilter,
    public val superTypesArgumentRenderer: KaSuperTypesCallArgumentsRenderer,

    public val bodyMemberScopeProvider: KaRendererBodyMemberScopeProvider,
    public val bodyMemberScopeSorter: KaRendererBodyMemberScopeSorter,

    public val functionLikeBodyRenderer: KaFunctionLikeBodyRenderer,
    public val variableInitializerRenderer: KaVariableInitializerRenderer,
    public val parameterDefaultValueRenderer: KaParameterDefaultValueRenderer,
    public val accessorBodyRenderer: KaPropertyAccessorBodyRenderer,

    public val returnTypeRenderer: KaCallableReturnTypeRenderer,
    public val callableReceiverRenderer: KaCallableReceiverRenderer,

    public val valueParametersRenderer: KaCallableParameterRenderer,
    public val typeParametersRenderer: KaTypeParametersRenderer,
    public val typeParametersFilter: KaTypeParameterRendererFilter,

    public val callableSignatureRenderer: KaCallableSignatureRenderer,

    public val anonymousFunctionRenderer: KaAnonymousFunctionSymbolRenderer,
    public val backingFieldRenderer: KaBackingFieldSymbolRenderer,
    public val constructorRenderer: KaConstructorSymbolRenderer,
    public val enumEntryRenderer: KaEnumEntrySymbolRenderer,
    public val namedFunctionRenderer: KaNamedFunctionSymbolRenderer,
    public val javaFieldRenderer: KaJavaFieldSymbolRenderer,
    public val localVariableRenderer: KaLocalVariableSymbolRenderer,
    public val getterRenderer: KaPropertyGetterSymbolRenderer,
    public val setterRenderer: KaPropertySetterSymbolRenderer,
    public val propertyRenderer: KaKotlinPropertySymbolRenderer,
    public val kotlinPropertyRenderer: KaKotlinPropertySymbolRenderer,
    public val syntheticJavaPropertyRenderer: KaSyntheticJavaPropertySymbolRenderer,
    public val valueParameterRenderer: KaValueParameterSymbolRenderer,
    public val samConstructorRenderer: KaSamConstructorSymbolRenderer,
    public val propertyAccessorsRenderer: KaPropertyAccessorsRenderer,
    public val destructuringDeclarationRenderer: KaDestructuringDeclarationRenderer,

    public val classInitializerRender: KaClassInitializerRenderer,
    public val namedClassRenderer: KaNamedClassSymbolRenderer,
    public val typeAliasRenderer: KaTypeAliasSymbolRenderer,
    public val anonymousObjectRenderer: KaAnonymousObjectSymbolRenderer,
    public val singleTypeParameterRenderer: KaSingleTypeParameterSymbolRenderer,
    public val returnTypeFilter: KaCallableReturnTypeFilter,

    public val scriptRenderer: KaScriptSymbolRenderer,
    public val scriptInitializerRenderer: KaScriptInitializerRenderer
) {

    public fun renderDeclaration(analysisSession: KaSession, symbol: KaDeclarationSymbol, printer: PrettyPrinter) {
        when (symbol) {
            is KaAnonymousObjectSymbol -> anonymousObjectRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaNamedClassSymbol -> namedClassRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaTypeAliasSymbol -> typeAliasRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaAnonymousFunctionSymbol -> anonymousFunctionRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaConstructorSymbol -> constructorRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaNamedFunctionSymbol -> namedFunctionRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaPropertyGetterSymbol -> getterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaPropertySetterSymbol -> setterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaSamConstructorSymbol -> samConstructorRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaBackingFieldSymbol -> backingFieldRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaEnumEntrySymbol -> enumEntryRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaValueParameterSymbol -> valueParameterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaReceiverParameterSymbol -> {}
            is KaJavaFieldSymbol -> javaFieldRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaLocalVariableSymbol -> localVariableRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaKotlinPropertySymbol -> kotlinPropertyRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaSyntheticJavaPropertySymbol -> syntheticJavaPropertyRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaTypeParameterSymbol -> singleTypeParameterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaClassInitializerSymbol -> classInitializerRender.renderClassInitializer(analysisSession, symbol, this, printer)
            is KaScriptSymbol -> scriptRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is KaDestructuringDeclarationSymbol -> destructuringDeclarationRenderer.renderSymbol(analysisSession, symbol, this, printer)
        }
    }

    public fun with(action: Builder.() -> Unit): KaDeclarationRenderer {
        val renderer = this
        return KaDeclarationRenderer {
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
            this.namedFunctionRenderer = renderer.namedFunctionRenderer
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
            this.namedClassRenderer = renderer.namedClassRenderer
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
        public operator fun invoke(action: Builder.() -> Unit): KaDeclarationRenderer =
            Builder().apply(action).build()
    }

    public open class Builder {
        public lateinit var returnTypeFilter: KaCallableReturnTypeFilter
        public lateinit var nameRenderer: KaDeclarationNameRenderer
        public lateinit var contextReceiversRenderer: KaContextReceiversRenderer
        public lateinit var keywordsRenderer: KaKeywordsRenderer
        public lateinit var codeStyle: KaRendererCodeStyle
        public lateinit var typeRenderer: KaTypeRenderer
        public lateinit var annotationRenderer: KaAnnotationRenderer
        public lateinit var modifiersRenderer: KaDeclarationModifiersRenderer
        public lateinit var declarationTypeApproximator: KaRendererTypeApproximator
        public lateinit var classifierBodyRenderer: KaClassifierBodyRenderer

        public lateinit var superTypeRenderer: KaSuperTypeRenderer
        public lateinit var superTypeListRenderer: KaSuperTypeListRenderer
        public lateinit var superTypesFilter: KaSuperTypesFilter
        public lateinit var superTypesArgumentRenderer: KaSuperTypesCallArgumentsRenderer

        public lateinit var bodyMemberScopeProvider: KaRendererBodyMemberScopeProvider
        public lateinit var bodyMemberScopeSorter: KaRendererBodyMemberScopeSorter

        public lateinit var functionLikeBodyRenderer: KaFunctionLikeBodyRenderer
        public lateinit var variableInitializerRenderer: KaVariableInitializerRenderer
        public lateinit var parameterDefaultValueRenderer: KaParameterDefaultValueRenderer
        public lateinit var accessorBodyRenderer: KaPropertyAccessorBodyRenderer

        public lateinit var returnTypeRenderer: KaCallableReturnTypeRenderer
        public lateinit var callableReceiverRenderer: KaCallableReceiverRenderer

        public lateinit var valueParametersRenderer: KaCallableParameterRenderer
        public lateinit var typeParametersRenderer: KaTypeParametersRenderer
        public lateinit var typeParametersFilter: KaTypeParameterRendererFilter
        public lateinit var callableSignatureRenderer: KaCallableSignatureRenderer

        public lateinit var anonymousFunctionRenderer: KaAnonymousFunctionSymbolRenderer
        public lateinit var backingFieldRenderer: KaBackingFieldSymbolRenderer
        public lateinit var constructorRenderer: KaConstructorSymbolRenderer
        public lateinit var enumEntryRenderer: KaEnumEntrySymbolRenderer
        public lateinit var namedFunctionRenderer: KaNamedFunctionSymbolRenderer
        public lateinit var javaFieldRenderer: KaJavaFieldSymbolRenderer
        public lateinit var localVariableRenderer: KaLocalVariableSymbolRenderer
        public lateinit var getterRenderer: KaPropertyGetterSymbolRenderer
        public lateinit var setterRenderer: KaPropertySetterSymbolRenderer
        public lateinit var propertyRenderer: KaKotlinPropertySymbolRenderer
        public lateinit var kotlinPropertyRenderer: KaKotlinPropertySymbolRenderer
        public lateinit var syntheticJavaPropertyRenderer: KaSyntheticJavaPropertySymbolRenderer
        public lateinit var valueParameterRenderer: KaValueParameterSymbolRenderer
        public lateinit var samConstructorRenderer: KaSamConstructorSymbolRenderer
        public lateinit var propertyAccessorsRenderer: KaPropertyAccessorsRenderer
        public lateinit var destructuringDeclarationRenderer: KaDestructuringDeclarationRenderer

        public lateinit var classInitializerRender: KaClassInitializerRenderer
        public lateinit var namedClassRenderer: KaNamedClassSymbolRenderer
        public lateinit var typeAliasRenderer: KaTypeAliasSymbolRenderer
        public lateinit var anonymousObjectRenderer: KaAnonymousObjectSymbolRenderer
        public lateinit var singleTypeParameterRenderer: KaSingleTypeParameterSymbolRenderer

        public lateinit var scriptRenderer: KaScriptSymbolRenderer
        public lateinit var scriptInitializerRenderer: KaScriptInitializerRenderer

        public fun build(): KaDeclarationRenderer = KaDeclarationRenderer(
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
            namedFunctionRenderer,
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
            namedClassRenderer,
            typeAliasRenderer,
            anonymousObjectRenderer,
            singleTypeParameterRenderer,
            returnTypeFilter,

            scriptRenderer,
            scriptInitializerRenderer,
        )
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaDeclarationRenderer' instead", ReplaceWith("KaDeclarationRenderer"))
public typealias KtDeclarationRenderer = KaDeclarationRenderer