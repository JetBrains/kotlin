/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.tree.generator.context.generatedType
import org.jetbrains.kotlin.fir.tree.generator.context.type
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

val sourceElementType = type("fir", "FirSourceElement")
val jumpTargetType = type("fir", "FirTarget")
val constKindType = generatedType("expressions", "FirConstKind")
val operationType = type("fir.expressions", "FirOperation")
val classKindType = type(ClassKind::class)
val invocationKindType = type(InvocationKind::class)
val varianceType = type(Variance::class)
val nameType = type(Name::class)
val visibilityType = type(Visibility::class)
val visibilitiesType = type(Visibilities::class)
val modalityType = type(Modality::class)
val fqNameType = type(FqName::class)
val classIdType = type(ClassId::class)
val annotationUseSiteTargetType = type(AnnotationUseSiteTarget::class)
val operationKindType = type("fir.expressions", "LogicOperationKind")
val coneKotlinTypeType = type(ConeKotlinType::class)
val whenSubjectType = type("fir", "FirWhenSubject")
val firSessionType = type("fir", "FirSession")
val emptyCfgReferenceType = generatedType("references.impl", "FirEmptyControlFlowGraphReference")
val noReceiverExpressionType = generatedType("expressions.impl", "FirNoReceiverExpression")
val implicitTypeRefType = generatedType("types.impl", "FirImplicitTypeRefImpl")
val firQualifierPartType = type("fir.types", "FirQualifierPart")
val coneClassErrorTypeType = type(ConeClassErrorType::class)
val simpleNamedReferenceType = generatedType("references.impl", "FirSimpleNamedReference")
val explicitThisReferenceType = generatedType("references.impl", "FirExplicitThisReference")
val explicitSuperReferenceType = generatedType("references.impl", "FirExplicitSuperReference")
val implicitBooleanTypeRefType = generatedType("types.impl", "FirImplicitBooleanTypeRef")
val implicitNothingTypeRefType = generatedType("types.impl", "FirImplicitNothingTypeRef")
val implicitStringTypeRefType = generatedType("types.impl", "FirImplicitStringTypeRef")
val implicitUnitTypeRefType = generatedType("types.impl", "FirImplicitUnitTypeRef")
val resolvePhaseType = type("fir.declarations", "FirResolvePhase")
val stubReferenceType = generatedType("references.impl", "FirStubReference")
val compositeTransformResultType = type("fir.visitors", "CompositeTransformResult")

val abstractFirBasedSymbolType = type("fir.symbols", "AbstractFirBasedSymbol")
val backingFieldSymbolType = type("fir.symbols.impl", "FirBackingFieldSymbol")
val delegateFieldSymbolType = type("fir.symbols.impl", "FirDelegateFieldSymbol")
val classSymbolType = type("fir.symbols.impl", "FirClassSymbol")
val typeParameterSymbolType = type("fir.symbols.impl", "FirTypeParameterSymbol")

val firScopeProviderType = type("fir.scopes", "FirScopeProvider")

val pureAbstractElementType = generatedType("FirPureAbstractElement")
val effectDeclarationType = type("fir.contracts.description", "ConeEffectDeclaration")
val emptyContractDescriptionType = generatedType("contracts.impl", "FirEmptyContractDescription")
val firDiagnosticType = generatedType("diagnostics", "FirDiagnostic")

