/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature.LocalVariableTargetedAnnotationOnDestructuring
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassKind.ENUM_ENTRY
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase.ANNOTATION_ARGUMENTS
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase.BODY_RESOLVE
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.extractEnumValueArgumentInfo
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getAnnotationWithResolvedArgumentsByClassId
import org.jetbrains.kotlin.fir.declarations.isJavaOrEnhancement
import org.jetbrains.kotlin.fir.declarations.utils.hasAnnotatableBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isCompanionBlockMember
import org.jetbrains.kotlin.fir.declarations.utils.isCompanionExtension
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isReplSnippetDeclaration
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.unwrapAndFlattenArgument
import org.jetbrains.kotlin.fir.isCatchParameter
import org.jetbrains.kotlin.fir.isForLoopParameter
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.propertyIfBackingField
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.AnnotationTargetList
import org.jetbrains.kotlin.resolve.AnnotationTargetListForDeprecation
import org.jetbrains.kotlin.resolve.AnnotationTargetLists

fun FirDeclaration.getTargetAnnotation(session: FirSession): FirAnnotation? {
    return getAnnotationByClassId(StandardClassIds.Annotations.Target, session)
}

fun FirClassLikeSymbol<*>.getTargetAnnotation(session: FirSession): FirAnnotation? {
    return getAnnotationWithResolvedArgumentsByClassId(StandardClassIds.Annotations.Target, session)
}

private val defaultAnnotationTargets = KotlinTarget.DEFAULT_TARGET_SET
private val defaultAnnotationTargetsWithExpression = KotlinTarget.DEFAULT_TARGET_SET + KotlinTarget.EXPRESSION

fun FirAnnotation.getAllowedAnnotationTargets(session: FirSession): Set<KotlinTarget> {
    if (annotationTypeRef is FirErrorTypeRef) return KotlinTarget.ALL_TARGET_SET
    val annotationClassSymbol = (this.annotationTypeRef.coneType as? ConeClassLikeType)
        ?.fullyExpandedType(session)?.lookupTag?.toSymbol(session) ?: return defaultAnnotationTargets
    annotationClassSymbol.lazyResolveToPhase(BODY_RESOLVE)
    return annotationClassSymbol.getAllowedAnnotationTargets(session)
}

fun FirRegularClass.getAllowedAnnotationTargets(session: FirSession): Set<KotlinTarget> {
    return symbol.getAllowedAnnotationTargets(session)
}

fun FirClassLikeSymbol<*>.getAllowedAnnotationTargets(session: FirSession): Set<KotlinTarget> {
    lazyResolveToPhase(ANNOTATION_ARGUMENTS)
    // In KT-67014, we decided to allow EXPRESSION targets for Java annotations
    val targetAnnotation = getTargetAnnotation(session)
        ?: return if (isJavaOrEnhancement) defaultAnnotationTargetsWithExpression else defaultAnnotationTargets
    val arguments =
        targetAnnotation.findArgumentByName(StandardClassIds.Annotations.ParameterNames.targetAllowedTargets)?.unwrapAndFlattenArgument(flattenArrays = true).orEmpty()

    return arguments.mapNotNullTo(mutableSetOf()) { argument ->
        val targetName = argument.extractEnumValueArgumentInfo()?.enumEntryName?.asString() ?: return@mapNotNullTo null
        KotlinTarget.entries.firstOrNull { target -> target.name == targetName }
    }.let {
        // In KT-67014, we decided to allow EXPRESSION targets for Java annotations
        if (isJavaOrEnhancement) it + KotlinTarget.EXPRESSION else it
    }
}

private val FirValueParameter.hasValOrVar: Boolean
    get() {
        val source = this.source ?: return false
        return source.getChild(KtTokens.VAL_VAR) != null
    }

private val FirCallableDeclaration.isMember get() = dispatchReceiverType != null

@OptIn(SymbolInternals::class)
context(sessionHolder: SessionHolder)
fun getActualTargetList(container: FirBasedSymbol<*>): AnnotationTargetList {
    return getActualTargetList(container.fir, sessionHolder.session)
}

context(sessionHolder: SessionHolder)
fun getActualTargetList(container: FirAnnotationContainer): AnnotationTargetList {
    return getActualTargetList(container, sessionHolder.session)
}

fun getActualTargetList(container: FirAnnotationContainer, session: FirSession): AnnotationTargetList {
    val annotated =
        if (container is FirBackingField) {
            when {
                !container.propertySymbol.hasAnnotatableBackingField -> container.propertyIfBackingField
                container.propertySymbol.getContainingClassSymbol()?.classKind == ClassKind.ANNOTATION_CLASS -> {
                    @OptIn(AnnotationTargetListForDeprecation::class)
                    return TargetLists.T_MEMBER_PROPERTY_IN_ANNOTATION
                }
                else -> container
            }
        } else container

    return when (annotated) {
        is FirRegularClass -> {
            AnnotationTargetList(
                KotlinTarget.classActualTargets(
                    annotated.classKind, annotated.isInner, annotated.isCompanion,
                    isLocalClass = annotated.isReplSnippetDeclaration != true && annotated.isLocal
                )
            )
        }
        is FirEnumEntry -> AnnotationTargetList(
            KotlinTarget.classActualTargets(ENUM_ENTRY, annotated.isInner, isCompanionObject = false, isLocalClass = false)
        )
        is FirProperty -> {
            when {
                annotated.symbol is FirLocalPropertySymbol ->
                    when {
                        annotated.name == SpecialNames.DESTRUCT -> if (session.languageVersionSettings.supportsFeature(
                                LocalVariableTargetedAnnotationOnDestructuring
                            )) {
                            TargetLists.T_DESTRUCTURING_DECLARATION_NEW
                        } else {
                            TargetLists.T_DESTRUCTURING_DECLARATION
                        }
                        annotated.isCatchParameter == true -> TargetLists.T_CATCH_PARAMETER
                        annotated.isForLoopParameter == true -> TargetLists.T_VALUE_PARAMETER_WITHOUT_VAL
                        else -> TargetLists.T_LOCAL_VARIABLE
                    }
                annotated.isMember ->
                    if (annotated.source?.kind == KtFakeSourceElementKind.PropertyFromParameter) {
                        TargetLists.T_VALUE_PARAMETER_WITH_VAL
                    } else {
                        TargetLists.T_MEMBER_PROPERTY(
                            backingField = annotated.hasAnnotatableBackingField,
                            delegate = annotated.delegate != null,
                            isCompanionMember = false,
                        )
                    }
                annotated.isCompanionBlockMember -> TargetLists.T_MEMBER_PROPERTY(
                    backingField = annotated.hasAnnotatableBackingField,
                    delegate = annotated.delegate != null,
                    isCompanionMember = true,
                )
                else ->
                    TargetLists.T_TOP_LEVEL_PROPERTY(
                        backingField = annotated.hasAnnotatableBackingField,
                        delegate = annotated.delegate != null,
                        isCompanionExtension = annotated.isCompanionExtension,
                    )
            }
        }
        is FirValueParameter -> {
            when {
                annotated.hasValOrVar -> TargetLists.T_VALUE_PARAMETER_WITH_VAL
                else -> TargetLists.T_VALUE_PARAMETER_WITHOUT_VAL
            }
        }
        is FirConstructor -> TargetLists.T_CONSTRUCTOR
        is FirAnonymousFunction -> {
            TargetLists.T_FUNCTION_EXPRESSION
        }
        is FirNamedFunction -> {
            when {
                annotated.status.visibility == Visibilities.Local -> TargetLists.T_LOCAL_FUNCTION
                annotated.isCompanionBlockMember -> TargetLists.T_COMPANION_MEMBER_FUNCTION
                annotated.isMember -> TargetLists.T_MEMBER_FUNCTION
                annotated.isCompanionExtension -> TargetLists.T_COMPANION_EXTENSION_FUNCTION
                else -> TargetLists.T_TOP_LEVEL_FUNCTION
            }
        }
        is FirTypeAlias -> TargetLists.T_TYPEALIAS
        is FirPropertyAccessor -> if (annotated.isGetter) TargetLists.T_PROPERTY_GETTER else TargetLists.T_PROPERTY_SETTER
        is FirBackingField -> TargetLists.T_BACKING_FIELD
        is FirFile -> TargetLists.T_FILE
        is FirTypeParameter -> TargetLists.T_TYPE_PARAMETER
        is FirReceiverParameter -> TargetLists.T_TYPE_REFERENCE
        is FirAnonymousInitializer -> TargetLists.T_INITIALIZER
        is FirAnonymousObject ->
            if (annotated.source?.kind == KtFakeSourceElementKind.EnumInitializer) {
                AnnotationTargetList(
                    KotlinTarget.classActualTargets(
                        ENUM_ENTRY,
                        isInnerClass = false,
                        isCompanionObject = false,
                        isLocalClass = false
                    )
                )
            } else {
                TargetLists.T_OBJECT_LITERAL
            }
//            TODO, KT-59819: properly implement this case
//            is KtLambdaExpression -> TargetLists.T_FUNCTION_LITERAL
        else -> TargetLists.EMPTY
    }
}

private typealias TargetLists = AnnotationTargetLists
