/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.generator

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirEffectiveVisibility
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DiagnosticList
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DiagnosticParameter
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.expressions.WhenMissingCase
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
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf

object HLDiagnosticConverter {
    fun convert(diagnosticList: DiagnosticList): HLDiagnosticList =
        HLDiagnosticList(diagnosticList.diagnostics.map(::convertDiagnostic))

    private fun convertDiagnostic(diagnostic: Diagnostic): HLDiagnostic =
        HLDiagnostic(
            original = diagnostic,
            className = diagnostic.getHLDiagnosticClassName(),
            implClassName = diagnostic.getHLDiagnosticImplClassName(),
            parameters = diagnostic.parameters.mapIndexed(::convertParameter)
        )

    private fun convertParameter(index: Int, diagnosticParameter: DiagnosticParameter): HLDiagnosticParameter {
        val conversion = FirToKtConversionCreator.creatConversion(diagnosticParameter.type)
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

    private fun Diagnostic.getHLDiagnosticClassName() =
        name.toLowerCase()
            .split('_')
            .joinToString(separator = "") {
                it.capitalize()
            }

    private fun Diagnostic.getHLDiagnosticImplClassName() =
        "${getHLDiagnosticClassName()}Impl"

}


private object FirToKtConversionCreator {
    fun creatConversion(type: KType): HLParameterConversion {
        val kClass = type.classifier as KClass<*>
        return tryMapAllowedType(kClass)
            ?: tryMapPsiElementType(type, kClass)
            ?: tryMapFirTypeToKtType(kClass)
            ?: tryMapCollectionType(type, kClass)
            ?: error("Unsupported type $type, consider add corresponding mapping")
    }

    private fun tryMapFirTypeToKtType(kClass: KClass<*>): HLParameterConversion? {
        return typeMapping[kClass]
    }

    private fun tryMapAllowedType(kClass: KClass<*>): HLParameterConversion? {
        if (kClass in allowedTypesWithoutTypeParams) return HLIdParameterConversion
        return null
    }

    private fun tryMapCollectionType(type: KType, kClass: KClass<*>): HLParameterConversion? {
        if (kClass.isSubclassOf(Collection::class)) {
            val elementType = type.arguments.single().type ?: return HLIdParameterConversion
            return HLMapParameterConversion(
                parameterName = elementType.kClass.simpleName!!.decapitalize(),
                mappingConversion = creatConversion(elementType)
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
            "firSymbolBuilder.buildClassLikeSymbol({0})",
            KtClassLikeSymbol::class.createType()
        ),
        FirClassLikeSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildClassLikeSymbol({0}.fir as FirClass<*>)",
            KtClassLikeSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirClass")
        ),
        FirMemberDeclaration::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0} as FirDeclaration)",
            KtSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirDeclaration")
        ),
        FirCallableDeclaration::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildCallableSymbol({0} as FirCallableDeclaration)",
            KtCallableSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration")
        ),
        FirTypeParameterSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildTypeParameterSymbol({0}.fir as FirTypeParameter)",
            KtTypeParameterSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirTypeParameter")
        ),
        FirEffectiveVisibility::class to HLFunctionCallConversion(
            "{0}.toVisibility()",
            Visibility::class.createType()
        ),
        ConeKotlinType::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildKtType({0})",
            KtType::class.createType()
        ),
        FirTypeRef::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildKtType({0})",
            KtType::class.createType()
        ),
        FirPropertySymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildVariableSymbol({0}.fir as FirProperty)",
            KtVariableSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirProperty")
        ),
        WhenMissingCase::class to HLFunctionCallConversion(
            """TODO("WhenMissingCase conversion is not supported yet")""",
            Any::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.expressions.WhenMissingCase")
        ),
    )

    private val allowedTypesWithoutTypeParams = setOf(
        String::class,
        Int::class,
        Name::class,
        EventOccurrencesRange::class,
        KtModifierKeywordToken::class,
        Visibility::class,
    )

    private val KType.kClass: KClass<*>
        get() = classifier as KClass<*>
}