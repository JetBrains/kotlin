/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.generator

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.fir.FirEffectiveVisibility
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DiagnosticData
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DiagnosticList
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DiagnosticParameter
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf

object HLDiagnosticConverter {
    fun convert(diagnosticList: DiagnosticList): HLDiagnosticList =
        HLDiagnosticList(diagnosticList.allDiagnostics.map(::convertDiagnostic))

    private fun convertDiagnostic(diagnostic: DiagnosticData): HLDiagnostic =
        HLDiagnostic(
            original = diagnostic,
            className = diagnostic.getHLDiagnosticClassName(),
            implClassName = diagnostic.getHLDiagnosticImplClassName(),
            parameters = diagnostic.parameters.mapIndexed(::convertParameter)
        )

    private fun convertParameter(index: Int, diagnosticParameter: DiagnosticParameter): HLDiagnosticParameter {
        val conversion = FirToKtConversionCreator.createConversion(diagnosticParameter.type)
        val convertedType = conversion.convertType(diagnosticParameter.type)
        return HLDiagnosticParameter(
            name = diagnosticParameter.name,
            conversion = conversion,
            originalParameterName = ('a' + index).toString(),
            type = convertedType,
            original = diagnosticParameter,
            importsToAdd = conversion.importsToAdd
        )
    }

    private fun DiagnosticData.getHLDiagnosticClassName() =
        name.toLowerCase()
            .split('_')
            .joinToString(separator = "") {
                it.capitalize()
            }

    private fun DiagnosticData.getHLDiagnosticImplClassName() =
        "${getHLDiagnosticClassName()}Impl"

}


private object FirToKtConversionCreator {
    fun createConversion(type: KType): HLParameterConversion {
        val kClass = type.classifier as KClass<*>
        return tryMapAllowedType(kClass)
            ?: tryMapPsiElementType(type, kClass)
            ?: tryMapFirTypeToKtType(kClass)
            ?: tryMapPlatformType(type, kClass)
            ?: error("Unsupported type $type, consider add corresponding mapping")
    }

    private fun tryMapFirTypeToKtType(kClass: KClass<*>): HLParameterConversion? {
        return typeMapping[kClass]
    }

    private fun tryMapAllowedType(kClass: KClass<*>): HLParameterConversion? {
        if (kClass in allowedTypesWithoutTypeParams) return HLIdParameterConversion
        return null
    }

    private fun tryMapPlatformType(type: KType, kClass: KClass<*>): HLParameterConversion? {
        if (kClass.isSubclassOf(Collection::class)) {
            val elementType = type.arguments.single().type ?: return HLIdParameterConversion
            return HLMapParameterConversion(
                parameterName = elementType.kClass.simpleName!!.decapitalize(),
                mappingConversion = createConversion(elementType)
            )
        }
        if (kClass.isSubclassOf(Pair::class)) {
            val first = type.arguments.getOrNull(0)?.type ?: return HLIdParameterConversion
            val second = type.arguments.getOrNull(1)?.type ?: return HLIdParameterConversion
            return HLPairParameterConversion(
                mappingConversionFirst = createConversion(first),
                mappingConversionSecond = createConversion(second)
            )
        }
        return null
    }

    private fun tryMapPsiElementType(type: KType, kClass: KClass<*>): HLParameterConversion? {
        if (kClass.isSubclassOf(PsiElement::class)) {
            return HLIdParameterConversion
        }
        return null
    }

    private val typeMapping: Map<KClass<*>, HLFunctionCallConversion> = mapOf(
        AbstractFirBasedSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0}.fir as FirDeclaration)",
            KtSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirDeclaration")
        ),
        FirClass::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildClassLikeSymbol({0})",
            KtClassLikeSymbol::class.createType()
        ),
        FirExpression::class to HLFunctionCallConversion(
            "{0}.source!!.psi as KtExpression",
            KtExpression::class.createType(),
            importsToAdd = listOf(
                "org.jetbrains.kotlin.psi.KtExpression",
                "org.jetbrains.kotlin.fir.psi"
            )
        ),
        FirClassLikeSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildClassLikeSymbol({0}.fir as FirClass<*>)",
            KtClassLikeSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirClass")
        ),
        FirMemberDeclaration::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0} as FirDeclaration)",
            KtSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirDeclaration")
        ),
        FirCallableDeclaration::class to HLFunctionCallConversion(
            "firSymbolBuilder.callableBuilder.buildCallableSymbol({0} as FirCallableDeclaration)",
            KtCallableSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration")
        ),
        FirTypeParameterSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol({0}.fir)",
            KtTypeParameterSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirTypeParameter")
        ),
        FirEffectiveVisibility::class to HLFunctionCallConversion(
            "{0}.toVisibility()",
            Visibility::class.createType()
        ),
        ConeKotlinType::class to HLFunctionCallConversion(
            "firSymbolBuilder.typeBuilder.buildKtType({0})",
            KtType::class.createType()
        ),
        FirTypeRef::class to HLFunctionCallConversion(
            "firSymbolBuilder.typeBuilder.buildKtType({0})",
            KtType::class.createType()
        ),
        FirPropertySymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.variableLikeBuilder.buildVariableSymbol({0}.fir as FirProperty)",
            KtVariableSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirProperty")
        ),
    )

    private val allowedTypesWithoutTypeParams = setOf(
        String::class,
        Int::class,
        Name::class,
        EventOccurrencesRange::class,
        KtModifierKeywordToken::class,
        Visibility::class,
        WhenMissingCase::class,
        ForbiddenNamedArgumentsTarget::class,
        LanguageFeature::class,
        LanguageVersionSettings::class,
    )

    private val KType.kClass: KClass<*>
        get() = classifier as KClass<*>
}
