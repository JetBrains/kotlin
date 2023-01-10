/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifier
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.checkers.isRecursiveValueClassType
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

object FirValueClassDeclarationChecker : FirRegularClassChecker() {

    private val boxAndUnboxNames = setOf("box", "unbox")
    private val equalsAndHashCodeNames = setOf("equals", "hashCode")
    private val javaLangFqName = FqName("java.lang")
    private val cloneableFqName = FqName("Cloneable")
    private val jvmInlineAnnotationFqName = FqName("kotlin.jvm.JvmInline")

    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.symbol.isInlineOrValueClass()) {
            return
        }

        if (declaration.isInner || declaration.isLocal) {
            reporter.reportOn(declaration.source, FirErrors.VALUE_CLASS_NOT_TOP_LEVEL, context)
        }

        if (declaration.modality != Modality.FINAL) {
            if (declaration.modality == Modality.SEALED) {
                if (!context.languageVersionSettings.supportsFeature(LanguageFeature.SealedInlineClasses)) {
                    reporter.reportOn(
                        declaration.getModifier(KtTokens.SEALED_KEYWORD)?.source,
                        FirErrors.UNSUPPORTED_FEATURE,
                        LanguageFeature.SealedInlineClasses to context.languageVersionSettings,
                        context
                    )
                }
            } else {
                reporter.reportOn(declaration.source, FirErrors.VALUE_CLASS_NOT_FINAL, context)
            }
        }

        // TODO check absence of context receivers when FIR infrastructure is ready

        for (supertypeEntry in declaration.superTypeRefs) {
            if (supertypeEntry.toRegularClassSymbol(context.session)?.let {
                    it.isInterface || (it.isSealedInlineClass() &&
                            context.languageVersionSettings.supportsFeature(LanguageFeature.SealedInlineClasses))
                } != true
            ) {
                reporter.reportOn(supertypeEntry.source, FirErrors.VALUE_CLASS_CANNOT_EXTEND_CLASSES, context)
            }
        }

        if (declaration.isSubtypeOfCloneable(context.session)) {
            reporter.reportOn(declaration.source, FirErrors.VALUE_CLASS_CANNOT_BE_CLONEABLE, context)
        }

        var primaryConstructor: FirConstructor? = null
        var primaryConstructorParametersByName = mapOf<Name, FirValueParameter>()
        val primaryConstructorPropertiesByName = mutableMapOf<Name, FirProperty>()
        var primaryConstructorParametersSymbolsSet = setOf<FirValueParameterSymbol>()

        for (innerDeclaration in declaration.declarations) {
            when (innerDeclaration) {
                is FirConstructor -> {
                    when {
                        innerDeclaration.isPrimary -> {
                            primaryConstructor = innerDeclaration
                            primaryConstructorParametersByName = innerDeclaration.valueParameters.associateBy { it.name }
                            primaryConstructorParametersSymbolsSet =
                                primaryConstructorParametersByName.map { (_, parameter) -> parameter.symbol }.toSet()
                        }

                        innerDeclaration.body != null -> {
                            val body = innerDeclaration.body!!
                            reporter.reportOn(
                                body.source, FirErrors.SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS, context
                            )
                        }
                    }
                }

                is FirRegularClass -> {
                    if (innerDeclaration.isInner) {
                        reporter.reportOn(innerDeclaration.source, FirErrors.INNER_CLASS_INSIDE_VALUE_CLASS, context)
                    }
                }

                is FirSimpleFunction -> {
                    val functionName = innerDeclaration.name.asString()

                    if (functionName in boxAndUnboxNames
                        || (functionName in equalsAndHashCodeNames
                                && !context.languageVersionSettings.supportsFeature(LanguageFeature.CustomEqualsInValueClasses))
                    ) {
                        reporter.reportOn(
                            innerDeclaration.source, FirErrors.RESERVED_MEMBER_INSIDE_VALUE_CLASS, functionName, context
                        )
                    }
                }

                is FirField -> {
                    if (innerDeclaration.isSynthetic) {
                        val symbol = innerDeclaration.initializer?.toResolvedCallableSymbol()
                        if (context.languageVersionSettings.supportsFeature(LanguageFeature.InlineClassImplementationByDelegation) &&
                            symbol != null && symbol in primaryConstructorParametersSymbolsSet
                        ) {
                            continue
                        }
                        val delegatedTypeRefSource = (innerDeclaration.returnTypeRef as FirResolvedTypeRef).delegatedTypeRef?.source
                        reporter.reportOn(
                            delegatedTypeRefSource,
                            FirErrors.VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION,
                            context
                        )
                    }
                }

                is FirProperty -> {
                    if (innerDeclaration.isRelatedToParameter(primaryConstructorParametersByName[innerDeclaration.name])) {
                        primaryConstructorPropertiesByName[innerDeclaration.name] = innerDeclaration
                    } else {
                        when {
                            innerDeclaration.delegate != null ->
                                reporter.reportOn(
                                    innerDeclaration.delegate!!.source,
                                    FirErrors.DELEGATED_PROPERTY_INSIDE_VALUE_CLASS,
                                    context
                                )

                            innerDeclaration.hasBackingField &&
                                    innerDeclaration.source?.kind !is KtFakeSourceElementKind -> {
                                reporter.reportOn(
                                    innerDeclaration.source, FirErrors.PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS,
                                    context
                                )
                            }
                        }
                    }
                }

                else -> {}
            }
        }

        val isNoinlineChildOfSealedInlineClass = declaration.isChildOfSealedInlineClass(context.session) &&
                declaration.annotations.none { it.fqName(context.session) == jvmInlineAnnotationFqName }

        if (declaration.modality != Modality.SEALED && !isNoinlineChildOfSealedInlineClass) {
            if (primaryConstructor?.source?.kind !is KtRealSourceElementKind) {
                reporter.reportOn(declaration.source, FirErrors.ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS, context)
                return
            }
            if (context.languageVersionSettings.supportsFeature(LanguageFeature.ValueClasses)) {
                if (primaryConstructorParametersByName.isEmpty()) {
                    reporter.reportOn(primaryConstructor.source, FirErrors.VALUE_CLASS_EMPTY_CONSTRUCTOR, context)
                    return
                }
            } else if (primaryConstructorParametersByName.size != 1) {
                reporter.reportOn(primaryConstructor.source, FirErrors.INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE, context)
                return
            }
        }

        if (declaration.modality == Modality.SEALED &&
            context.languageVersionSettings.supportsFeature(LanguageFeature.SealedInlineClasses) &&
            primaryConstructor?.source?.kind is KtRealSourceElementKind
        ) {
            reporter.reportOn(declaration.source, FirErrors.SEALED_INLINE_CLASS_WITH_PRIMARY_CONSTRUCTOR, context)
            return
        }

        if (primaryConstructor == null) return

        for ((name, primaryConstructorParameter) in primaryConstructorParametersByName) {
            when {
                primaryConstructorParameter.isNotFinalReadOnly(primaryConstructorPropertiesByName[name]) ->
                    reporter.reportOn(
                        primaryConstructorParameter.source,
                        FirErrors.VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER,
                        context
                    )

                !context.languageVersionSettings.supportsFeature(LanguageFeature.GenericInlineClassParameter) &&
                        primaryConstructorParameter.returnTypeRef.coneType.let {
                            it is ConeTypeParameterType || it.isGenericArrayOfTypeParameter()
                        } -> {
                    reporter.reportOn(
                        primaryConstructorParameter.returnTypeRef.source,
                        FirErrors.UNSUPPORTED_FEATURE,
                        LanguageFeature.GenericInlineClassParameter to context.languageVersionSettings,
                        context
                    )
                }

                primaryConstructorParameter.returnTypeRef.isInapplicableParameterType() -> {
                    reporter.reportOn(
                        primaryConstructorParameter.returnTypeRef.source,
                        FirErrors.VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE,
                        primaryConstructorParameter.returnTypeRef.coneType,
                        context
                    )
                }

                primaryConstructorParameter.returnTypeRef.coneType.isRecursiveValueClassType(context.session) -> {
                    reporter.reportOn(
                        primaryConstructorParameter.returnTypeRef.source, FirErrors.VALUE_CLASS_CANNOT_BE_RECURSIVE,
                        context
                    )
                }

                declaration.multiFieldValueClassRepresentation != null && primaryConstructorParameter.defaultValue != null -> {
                    // todo fix when inline arguments are supported
                    reporter.reportOn(
                        primaryConstructorParameter.defaultValue!!.source,
                        FirErrors.MULTI_FIELD_VALUE_CLASS_PRIMARY_CONSTRUCTOR_DEFAULT_PARAMETER,
                        context
                    )
                }
            }
        }

        if (context.languageVersionSettings.supportsFeature(LanguageFeature.CustomEqualsInValueClasses)) {
            val (equalsFromAnyOverriding, typedEquals) = run {
                var equalsFromAnyOverriding: FirSimpleFunction? = null
                var typedEquals: FirSimpleFunction? = null
                declaration.declarations.forEach {
                    if (it !is FirSimpleFunction) {
                        return@forEach
                    }
                    if (it.isEquals()) equalsFromAnyOverriding = it
                    if (it.isTypedEqualsInValueClass(context.session)) typedEquals = it
                }
                equalsFromAnyOverriding to typedEquals
            }
            if (typedEquals != null) {
                if (typedEquals.typeParameters.isNotEmpty()) {
                    reporter.reportOn(
                        typedEquals.source,
                        FirErrors.TYPE_PARAMETERS_NOT_ALLOWED,
                        context
                    )
                }
                val singleParameterReturnTypeRef = typedEquals.valueParameters.single().returnTypeRef
                if (singleParameterReturnTypeRef.coneType.typeArguments.any { !it.isStarProjection }) {
                    reporter.reportOn(singleParameterReturnTypeRef.source, FirErrors.TYPE_ARGUMENT_ON_TYPED_VALUE_CLASS_EQUALS, context)
                }
            }

            if (equalsFromAnyOverriding != null && typedEquals == null) {
                reporter.reportOn(
                    equalsFromAnyOverriding.source,
                    FirErrors.INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS,
                    declaration.defaultType().replaceArgumentsWithStarProjections(),
                    context
                )
            }
        }
    }

    private fun FirProperty.isRelatedToParameter(parameter: FirValueParameter?) =
        name == parameter?.name && source?.kind is KtFakeSourceElementKind

    private fun FirValueParameter.isNotFinalReadOnly(primaryConstructorProperty: FirProperty?): Boolean {
        if (primaryConstructorProperty == null) return true

        val isOpen = hasModifier(KtTokens.OPEN_KEYWORD)

        return isVararg || !primaryConstructorProperty.isVal || isOpen
    }

    private fun FirTypeRef.isInapplicableParameterType() =
        isUnit || isNothing

    private fun ConeKotlinType.isGenericArrayOfTypeParameter(): Boolean {
        if (this.typeArguments.firstOrNull() is ConeStarProjection || !isPotentiallyArray())
            return false

        val arrayElementType = arrayElementType()?.type ?: return false
        return arrayElementType is ConeTypeParameterType ||
                arrayElementType.isGenericArrayOfTypeParameter()
    }

    private fun FirRegularClass.isSubtypeOfCloneable(session: FirSession): Boolean {
        if (classId.isCloneableId()) return true

        return lookupSuperTypes(this, lookupInterfaces = true, deep = true, session, substituteTypes = false).any { superType ->
            // Note: We check just classId here, so type substitution isn't needed   ^ (we aren't interested in type arguments)
            (superType as? ConeClassLikeType)?.fullyExpandedType(session)?.lookupTag?.classId?.isCloneableId() == true
        }
    }

    private fun ClassId.isCloneableId(): Boolean =
        relativeClassName == cloneableFqName &&
                (packageFqName == StandardClassIds.BASE_KOTLIN_PACKAGE || packageFqName == javaLangFqName)
}

object FirSealedInlineClassChildChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.symbol.isValueObject() && !declaration.isChildOfSealedInlineClass(context.session)) {
            reporter.reportOn(declaration.source, FirErrors.VALUE_OBJECT_NOT_SEALED_INLINE_CHILD, context)
        }

        if (!declaration.isChildOfSealedInlineClass(context.session)) return

        if (!declaration.isInline) {
            reporter.reportOn(declaration.source, FirErrors.SEALED_INLINE_CHILD_NOT_VALUE, context)
        }
    }
}

fun FirRegularClass.isChildOfSealedInlineClass(session: FirSession): Boolean = supertypeAsSealedInlineClassType(session) != null

fun FirRegularClass.supertypeAsSealedInlineClassType(session: FirSession): FirTypeRef? =
    superTypeRefs.find { superType ->
        superType.toRegularClassSymbol(session)?.isSealedInlineClass() == true
    }

fun FirRegularClassSymbol.isSealedInlineClass(): Boolean =
    isInline && classKind == ClassKind.CLASS && modality == Modality.SEALED
