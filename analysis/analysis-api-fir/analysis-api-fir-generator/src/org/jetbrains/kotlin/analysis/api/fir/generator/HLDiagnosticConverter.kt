/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.config.ApiVersion
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
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.types.Variance
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf

object HLDiagnosticConverter {
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

internal object FirToKtConversionCreator {
    fun createConversion(type: KType): HLParameterConversion {
        val nullable = type.isMarkedNullable
        val kClass = type.classifier as KClass<*>
        return tryMapAllowedType(kClass)
            ?: tryMapPsiElementType(kClass)
            ?: tryMapFirTypeToKtType(kClass, nullable)
            ?: tryMapPlatformType(type, kClass)
            ?: error("Unsupported type $type, consider add corresponding mapping")
    }

    fun getAllConverters(conversionForCollectionValues: HLParameterConversion): Map<KClass<*>, HLParameterConversion> {
        return buildMap {
            putAll(typeMapping)
            put(
                Map::class,
                HLMapParameterConversion(
                    "key",
                    "value",
                    conversionForCollectionValues,
                    conversionForCollectionValues
                )
            )
            put(
                Collection::class,
                HLCollectionParameterConversion("value", conversionForCollectionValues)
            )
            put(
                Pair::class,
                HLPairParameterConversion(
                    conversionForCollectionValues,
                    conversionForCollectionValues
                )
            )
        }
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
        KtSourceElement::class to HLFunctionCallConversion(
            "({0} as? KtPsiSourceElement)?.psi",
            PsiElement::class.createType(nullable = true),
            importsToAdd = listOf(
                "org.jetbrains.kotlin.psi",
                "org.jetbrains.kotlin.KtPsiSourceElement"
            )
        ),
    )

    private val typeMapping: Map<KClass<*>, HLFunctionCallConversion> = mapOf(
        // ------------------ symbols ------------------
        FirRegularClass::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildClassLikeSymbol({0}.symbol) as KtNamedClassOrObjectSymbol",
            KtNamedClassOrObjectSymbol::class.createType(),
            importsToAdd = listOf(
                "org.jetbrains.kotlin.fir.declarations.FirRegularClass",
                "org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol"
            )
        ),
        FirValueParameterSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0})",
            KtSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirDeclaration")
        ),
        FirEnumEntrySymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0})",
            KtSymbol::class.createType(),
        ),
        FirRegularClassSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildClassLikeSymbol({0})",
            KtClassLikeSymbol::class.createType()
        ),
        FirNamedFunctionSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol({0})",
            KtFunctionLikeSymbol::class.createType()
        ),
        FirPropertySymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.variableLikeBuilder.buildVariableSymbol({0})",
            KtVariableSymbol::class.createType()
        ),
        FirBackingFieldSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.variableLikeBuilder.buildVariableSymbol({0}.fir.propertySymbol)",
            KtVariableSymbol::class.createType()
        ),
        FirVariableSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.variableLikeBuilder.buildVariableLikeSymbol({0})",
            KtVariableLikeSymbol::class.createType()
        ),
        FirTypeParameterSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol({0})",
            KtTypeParameterSymbol::class.createType()
        ),
        FirCallableSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.callableBuilder.buildCallableSymbol({0})",
            KtCallableSymbol::class.createType()
        ),
        FirClassSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildClassLikeSymbol({0})",
            KtClassLikeSymbol::class.createType()
        ),
        FirClassLikeSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildClassLikeSymbol({0})",
            KtClassLikeSymbol::class.createType()
        ),
        FirBasedSymbol::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0})",
            KtSymbol::class.createType()
        ),
        // ------------------ FIR elements ------------------
        FirClass::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildClassLikeSymbol({0}.symbol)",
            KtClassLikeSymbol::class.createType()
        ),
        FirTypeParameter::class to HLFunctionCallConversion(
            "firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol({0}.symbol)",
            KtTypeParameterSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirTypeParameter")
        ),
        FirValueParameter::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0}.symbol)",
            KtSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirDeclaration")
        ),
        FirFunction::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0})",
            KtSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirFunction")
        ),
        FirCallableDeclaration::class to HLFunctionCallConversion(
            "firSymbolBuilder.callableBuilder.buildCallableSymbol({0}.symbol)",
            KtCallableSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration")
        ),
        FirMemberDeclaration::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0} as FirDeclaration)",
            KtSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirDeclaration")
        ),
        FirDeclaration::class to HLFunctionCallConversion(
            "firSymbolBuilder.buildSymbol({0})",
            KtSymbol::class.createType(),
            importsToAdd = listOf("org.jetbrains.kotlin.fir.declarations.FirDeclaration")
        ),
        FirQualifiedAccessExpression::class to HLFunctionCallConversion(
            "{0}.source!!.psi as KtExpression",
            KtExpression::class.createType(),
            importsToAdd = listOf(
                "org.jetbrains.kotlin.psi.KtExpression",
                "org.jetbrains.kotlin.psi"
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
        // ------------------ other ------------------

        ConeKotlinType::class to HLFunctionCallConversion(
            "firSymbolBuilder.typeBuilder.buildKtType({0})",
            KtType::class.createType()
        ),
        FirTypeRef::class to HLFunctionCallConversion(
            "firSymbolBuilder.typeBuilder.buildKtType({0})",
            KtType::class.createType()
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
        ExpectActualCompatibility::class,
        ExpectActualCompatibility.MismatchOrIncompatible::class,
        ExpectActualAnnotationsIncompatibilityType::class,
        DeprecationInfo::class,
        ApiVersion::class,
        CallableId::class,
        ClassKind::class,
        FunctionTypeKind::class,
        VersionRequirement.Version::class,
        IncompatibleVersionErrorData::class,
    )

    private val KType.kClass: KClass<*>
        get() = classifier as KClass<*>
}
