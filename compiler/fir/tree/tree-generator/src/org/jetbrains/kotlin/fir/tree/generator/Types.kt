/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.tree.generator.context.generatedType
import org.jetbrains.kotlin.fir.tree.generator.context.type
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.SmartcastStability
import org.jetbrains.kotlin.types.Variance

val sourceElementType = type(KtSourceElement::class)
val sourceFileType = type(KtSourceFile::class)
val sourceFileLinesMappingType = type(KtSourceFileLinesMapping::class)
val jumpTargetType = type("fir", "FirTarget")
val constKindType = type("types", "ConstantValueKind")
val operationType = type("fir.expressions", "FirOperation")
val classKindType = type(ClassKind::class)
val eventOccurrencesRangeType = type(EventOccurrencesRange::class)
val inlineStatusType = type("fir.declarations", "InlineStatus")
val varianceType = type(Variance::class)
val nameType = type(Name::class)
val visibilityType = type(Visibility::class)
val effectiveVisibilityType = type("descriptors", "EffectiveVisibility")
val modalityType = type(Modality::class)
val smartcastStabilityType = type(SmartcastStability::class)
val fqNameType = type(FqName::class)
val classIdType = type(ClassId::class)
val annotationUseSiteTargetType = type(AnnotationUseSiteTarget::class)
val operationKindType = type("fir.expressions", "LogicOperationKind")
val coneKotlinTypeType = type(ConeKotlinType::class)
val coneSimpleKotlinTypeType = type(ConeSimpleKotlinType::class)

val whenExpressionType = generatedType("expressions", "FirWhenExpression")
val expressionType = generatedType("expressions", "FirExpression")
val safeCallCheckedSubjectType = generatedType("expressions", "FirCheckedSafeCallSubject")

val whenRefType = generatedType("", "FirExpressionRef<FirWhenExpression>")
val safeCallOriginalReceiverReferenceType = generatedType("", "FirExpressionRef<FirExpression>")
val safeCallCheckedSubjectReferenceType = generatedType("", "FirExpressionRef<FirCheckedSafeCallSubject>")

val firModuleDataType = type("fir", "FirModuleData")
val emptyCfgReferenceType = generatedType("references.impl", "FirEmptyControlFlowGraphReference")
val noReceiverExpressionType = generatedType("expressions.impl", "FirNoReceiverExpression")
val implicitTypeRefType = generatedType("types.impl", "FirImplicitTypeRefImpl")
val firQualifierPartType = type("fir.types", "FirQualifierPart")
val ConeErrorTypeType = type(ConeErrorType::class)
val simpleNamedReferenceType = generatedType("references.impl", "FirSimpleNamedReference")
val explicitThisReferenceType = generatedType("references.impl", "FirExplicitThisReference")
val explicitSuperReferenceType = generatedType("references.impl", "FirExplicitSuperReference")
val implicitBooleanTypeRefType = generatedType("types.impl", "FirImplicitBooleanTypeRef")
val implicitNothingTypeRefType = generatedType("types.impl", "FirImplicitNothingTypeRef")
val implicitStringTypeRefType = generatedType("types.impl", "FirImplicitStringTypeRef")
val implicitUnitTypeRefType = generatedType("types.impl", "FirImplicitUnitTypeRef")
val resolvePhaseType = type("fir.declarations", "FirResolvePhase")
val propertyBodyResolveStateType = type("fir.declarations", "FirPropertyBodyResolveState")
val stubReferenceType = generatedType("references.impl", "FirStubReference")

val firBasedSymbolType = type("fir.symbols", "FirBasedSymbol")
val backingFieldSymbolType = type("fir.symbols.impl", "FirBackingFieldSymbol")
val delegateFieldSymbolType = type("fir.symbols.impl", "FirDelegateFieldSymbol")
val classSymbolType = type("fir.symbols.impl", "FirClassSymbol")
val classLikeSymbolType = type("fir.symbols.impl", "FirClassLikeSymbol<*>")
val regularClassSymbolType = type("fir.symbols.impl", "FirRegularClassSymbol")
val typeParameterSymbolType = type("fir.symbols.impl", "FirTypeParameterSymbol")
val emptyArgumentListType = type("fir.expressions", "FirEmptyArgumentList")
val firScopeProviderType = type("fir.scopes", "FirScopeProvider")

val pureAbstractElementType = generatedType("FirPureAbstractElement")
val coneEffectDeclarationType = type("fir.contracts.description", "ConeEffectDeclaration")
val emptyContractDescriptionType = generatedType("contracts.impl", "FirEmptyContractDescription")
val coneDiagnosticType = generatedType("diagnostics", "ConeDiagnostic")
val coneStubDiagnosticType = generatedType("diagnostics", "ConeStubDiagnostic")

val dslBuilderAnnotationType = generatedType("builder", "FirBuilderDsl")
val firImplementationDetailType = generatedType("FirImplementationDetail")
val declarationOriginType = generatedType("declarations", "FirDeclarationOrigin")
val declarationAttributesType = generatedType("declarations", "FirDeclarationAttributes")

val exhaustivenessStatusType = generatedType("expressions", "ExhaustivenessStatus")

val callableReferenceMappedArgumentsType = type("fir.resolve.calls", "CallableReferenceMappedArguments")

val functionCallOrigin = type("fir.expressions", "FirFunctionCallOrigin")

val resolvedDeclarationStatusImplType = type("fir.declarations.impl", "FirResolvedDeclarationStatusImpl")

val deprecationsPerUseSiteType = type("fir.declarations", "DeprecationsPerUseSite")
val emptyAnnotationArgumentMappingType = type("fir.expressions.impl", "FirEmptyAnnotationArgumentMapping")

val firPropertySymbolType = type("fir.symbols.impl", "FirPropertySymbol")
val errorTypeRefImplType = type("fir.types.impl", "FirErrorTypeRefImpl")

