/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.types.Variance
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf

object HLDiagnosticConverter {
    @OptIn(ExperimentalStdlibApi::class)
    fun convert(diagnosticList: DiagnosticList): HLDiagnosticList {
        return HLDiagnosticList(diagnosticList.allDiagnostics.flatMap(::convertDiagnostic))
    }

    private fun convertDiagnostic(diagnostic: DiagnosticData): List<HLDiagnostic> {
        return when (diagnostic){
            is RegularDiagnosticData -> listOf(
                HLDiagnostic(
                    original = diagnostic,
                    severity = null,
                    className = diagnostic.getHLDiagnosticClassName(),
                    implClassName = diagnostic.getHLDiagnosticImplClassName(),
                    parameters = diagnostic.parameters.mapIndexed(::convertParameter)
                )
            )
            is DeprecationDiagnosticData -> listOf(Severity.ERROR, Severity.WARNING).map {
                HLDiagnostic(
                    original = diagnostic,
                    severity = it,
                    className = diagnostic.getHLDiagnosticClassName(it),
                    implClassName = diagnostic.getHLDiagnosticImplClassName(it),
                    parameters = diagnostic.parameters.mapIndexed(::convertParameter)
                )
            }
        }
    }

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

    private fun RegularDiagnosticData.getHLDiagnosticClassName(): String = name.sanitizeName()

    private fun RegularDiagnosticData.getHLDiagnosticImplClassName(): String =
        "${getHLDiagnosticClassName()}Impl"

    private fun DeprecationDiagnosticData.getHLDiagnosticClassName(severity: Severity): String {
        val diagnosticName = "${name}_${severity.name}"
        return diagnosticName.sanitizeName()
    }

    private fun DeprecationDiagnosticData.getHLDiagnosticImplClassName(severity: Severity): String {
        return "${getHLDiagnosticClassName(severity)}Impl"
    }

    private fun String.sanitizeName(): String =
        lowercase()
            .split('_')
            .joinToString(separator = "") {
                it.replaceFirstChar(Char::uppercaseChar)
            }

}


private object FirToKtConversionCreator {
    fun createConversion(type: KType): HLParameterConversion {
        val nullable = type.isMarkedNullable
        val kClass = type.classifier as KClass<*>
        return tryMapAllowedType(kClass)
            ?: tryMapPsiElementType(kClass)
            ?: tryMapFirTypeToKtType(kClass, nullable)
            ?: tryMapPlatformType(type, kClass)
            ?: error("Unsupported type $type, consider add corresponding mapping")
    }

    private fun tryMapFirTypeToKtType(kClass: KClass<*>, nullable: Boolean): HLParameterConversion? {
        return if (nullable) {
            nullableTypeMapping[kClass] ?: typeMapping[kClass]
        } else {
            typeMapping[kClass]
        }
    }

    private fun tryMapAllowedType(kClass: KClass<*>): HLParameterConversion? {
        if (kClass in allowedTypesWithoutTypeParams) return HLIdParameterConversion
        return null
    }

    private fun KType.toParameterName(): String {
        return kClass.simpleName!!.replaceFirstChar(Char::lowercaseChar)
    }

    private fun tryMapPlatformType(type: KType, kClass: KClass<*>): HLParameterConversion? {
        if (kClass.isSubclassOf(Collection::class)) {
            val elementType = type.arguments.single().type ?: return HLIdParameterConversion
            return HLCollectionParameterConversion(
                parameterName = elementType.toParameterName(),
                mappingConversion = createConversion(elementType)
            )
        }
        if (kClass.isSubclassOf(Map::class)) {
            val keyType = type.arguments.getOrNull(0)?.type
            val valueType = type.arguments.getOrNull(1)?.type

            val keyConversion = keyType?.let { createConversion(it) } ?: HLIdParameterConversion
            val valueConversion = valueType?.let { createConversion(it) } ?: HLIdParameterConversion
            if (keyConversion.isTrivial && valueConversion.isTrivial) return HLIdParameterConversion
            return HLMapParameterConversion(
                keyName = keyType?.toParameterName() ?: "key",
                valueName = valueType?.toParameterName() ?: "value",
                mappingConversionForKeys = keyConversion,
                mappingConversionForValues = valueConversion
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

    private fun tryMapPsiElementType(kClass: KClass<*>): HLParameterConversion? {
        if (kClass.isSubclassOf(PsiElement::class)) {
            return HLIdParameterConversion
        }
        return null
    }

    private val nullableTypeMapping: Map<KClass<*>, HLFunctionCallConversion> = mapOf(
        FirExpression::class to HLFunctionCallConversion(
            "{0}?.source?.psi as? KtExpression",
            KtExpression::class.createType(nullable = true),
            importsToAdd = listOf(
                "org.jetbrains.kotlin.psi.KtExpression",
                "org.jetbrains.kotlin.psi"
            )
        ),
    )

    private val typeMapping: Map<KClass<*>, HLFunctionCallConversion> = mapOf(
        FirBasedSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0}.fir)",
            KtSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirDeclaration")
        ),
        FirClass::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildClassLikeSymbol({0})",
            KtClassLikeSymbol::class.createType()
        ),
        FirClassSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildClassLikeSymbol({0}.fir)",
            KtClassLikeSymbol::class.createType()
        ),
        FirRegularClass::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildClassLikeSymbol({0}) as KtNamedClassOrObjectSymbol",
            KtNamedClassOrObjectSymbol::class.createType(),
            importsToAdd = listOf(
                "org.jetbrains.kotlin.fir.declarations.FirRegularClass",
                "org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol"
            )
        ),
        FirExpression::class to HLFunctionCallConversion(
            "{0}.source!!.psi as KtExpression",
            KtExpression::class.createType(),
            importsToAdd = listOf(
                "org.jetbrains.kotlin.psi.KtExpression",
                "org.jetbrains.kotlin.psi"
            )
        ),
        FirQualifiedAccess::class to HLFunctionCallConversion(
            "{0}.source!!.psi as KtExpression",
            KtExpression::class.createType(),
            importsToAdd = listOf(
                "org.jetbrains.kotlin.psi.KtExpression",
                "org.jetbrains.kotlin.psi"
            )
        ),
        FirValueParameter::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0})",
            KtSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirDeclaration")
        ),
        FirValueParameterSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0}.fir)",
            KtSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirDeclaration")
        ),
        FirEnumEntrySymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0}.fir)",
            KtSymbol::class.createType(),
        ),
        FirClassLikeSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildClassLikeSymbol({0}.fir as FirClass)",
            KtClassLikeSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirClass")
        ),
        FirRegularClassSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildClassLikeSymbol({0}.fir)",
            KtClassLikeSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirRegularClass")
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
        FirCallableSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.callableBuilder.buildCallableSymbol({0}.fir)",
            KtCallableSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration")
        ),
        FirTypeParameterSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol({0}.fir)",
            KtTypeParameterSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirTypeParameter")
        ),
        FirTypeParameter::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol({0}.fir)",
            KtTypeParameterSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirTypeParameter")
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
            "firSymbolBuilder.variableLikeBuilder.buildVariableSymbol({0}.fir)",
            KtVariableSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirProperty")
        ),
        FirBackingFieldSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.variableLikeBuilder.buildVariableSymbol({0}.fir.propertySymbol.fir)",
            KtVariableSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirProperty")
        ),
        FirVariableSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.variableLikeBuilder.buildVariableLikeSymbol({0}.fir)",
            KtVariableLikeSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirVariable")
        ),
        FirDeclaration::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0})",
            KtSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirDeclaration")
        ),
        FirSimpleFunction::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0})",
            KtSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirSimpleFunction")
        ),
        FirNamedFunctionSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol({0}.fir)",
            KtFunctionLikeSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirSimpleFunction")
        ),
        KtSourceElement::class to HLFunctionCallConversion(
            "({0} as KtPsiSourceElement).psi",
            PsiElement::class.createType(),
            importsToAdd = listOf(
                "org.jetbrains.kotlin.psi",
                "org.jetbrains.kotlin.KtPsiSourceElement"
            )
        )
    )

    private val allowedTypesWithoutTypeParams = setOf(
        Boolean::class,
        String::class,
        Int::class,
        Name::class,
        EventOccurrencesRange::class,
        KtKeywordToken::class,
        KtModifierKeywordToken::class,
        Visibility::class,
        EffectiveVisibility::class,
        WhenMissingCase::class,
        ForbiddenNamedArgumentsTarget::class,
        LanguageFeature::class,
        LanguageVersionSettings::class,
        Variance::class,
        FqName::class,
        ClassId::class,
        FirModuleData::class,
        ExpectActualCompatibility.Incompatible::class,
        DeprecationInfo::class,
        CallableId::class,
        ClassKind::class,
    )

    private val KType.kClass: KClass<*>
        get() = classifier as KClass<*>
}
