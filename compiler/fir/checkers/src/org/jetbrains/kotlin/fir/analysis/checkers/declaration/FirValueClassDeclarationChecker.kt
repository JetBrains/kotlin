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
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.RecursionType.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitAnyTypeRef
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.VALUE_KEYWORD
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

sealed class FirValueClassDeclarationChecker(mppKind: MppCheckerKind) : FirRegularClassChecker(mppKind) {
    object Regular : FirValueClassDeclarationChecker(MppCheckerKind.Platform) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirRegularClass) {
            if (declaration.isExpect) return
            super.check(declaration)
        }
    }

    object ForExpectClass : FirValueClassDeclarationChecker(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirRegularClass) {
            if (!declaration.isExpect) return
            super.check(declaration)
        }
    }

    companion object {
        private val boxAndUnboxNames = setOf("box", "unbox")
        private val equalsAndHashCodeNames = setOf("equals", "hashCode")
        private val javaLangFqName = FqName("java.lang")
        private val cloneableFqName = FqName("Cloneable")
        private val recordFqName = FqName("Record")
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (declaration.classKind != ClassKind.CLASS || !declaration.symbol.isInlineOrValue) {
            return
        }

        val supportsFullValueClasses = LanguageFeature.FullValueClasses.isEnabled()
        val valueModifierPrefix = if (supportsFullValueClasses) "@JvmInline value" else "Value"
        val isFullValueClass = declaration.symbol.isFullValueClass

        if (declaration.isInner || declaration.isLocal) {
            reporter.reportOn(declaration.source, FirErrors.VALUE_CLASS_NOT_TOP_LEVEL)
        }

        if (isFullValueClass) {
            if (declaration.modality == Modality.OPEN) {
                reporter.reportOn(declaration.source, FirErrors.VALUE_CLASS_OPEN)
            }
        } else if (declaration.modality != Modality.FINAL) {
            reporter.reportOn(
                declaration.source,
                FirErrors.VALUE_CLASS_NOT_FINAL,
                valueModifierPrefix,
            )
        }

        if (declaration.contextParameters.isNotEmpty() && LanguageFeature.ContextReceivers.isEnabled()) {
            reporter.reportOn(declaration.source, FirErrors.VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS)
        }


        for (supertypeEntry in declaration.superTypeRefs) {
            if (supertypeEntry is FirImplicitAnyTypeRef || supertypeEntry is FirErrorTypeRef) continue
            val supertypeSymbol = supertypeEntry.toRegularClassSymbol(context.session) ?: continue
            if (supertypeSymbol.isInterface) continue
            if (!isFullValueClass) {
                reporter.reportOn(
                    supertypeEntry.source,
                    FirErrors.VALUE_CLASS_CANNOT_EXTEND_CLASSES,
                    valueModifierPrefix,
                )
            } else if (!supertypeSymbol.isFullValueClass && !supertypeSymbol.classId.isRecordId()) {
                reporter.reportOn(supertypeEntry.source, FirErrors.VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES)
            }
        }

        if (declaration.isSubtypeOfCloneable(context.session)) {
            reporter.reportOn(declaration.source, FirErrors.VALUE_CLASS_CANNOT_BE_CLONEABLE)
        }

        var primaryConstructor: FirConstructorSymbol? = null
        var primaryConstructorParametersByName = mapOf<Name, FirValueParameterSymbol>()
        val primaryConstructorPropertiesByName = hashMapOf<Name, FirPropertySymbol>()
        var primaryConstructorParametersSymbolsSet = setOf<FirValueParameterSymbol>()
        val isCustomEqualsSupported = LanguageFeature.CustomEqualsInValueClasses.isEnabled()

        declaration.constructors(context.session).forEach { innerDeclaration ->
            when {
                innerDeclaration.isPrimary -> {
                    primaryConstructor = innerDeclaration
                    primaryConstructorParametersByName = innerDeclaration.valueParameterSymbols.associateBy { it.name }
                    primaryConstructorParametersSymbolsSet = primaryConstructorParametersByName.values.toSet()
                }
            }
        }
        declaration.processAllDeclarations(context.session) { innerDeclaration ->
            when (innerDeclaration) {
                is FirRegularClassSymbol -> {
                    if (innerDeclaration.isInner && !isFullValueClass) {
                        reporter.reportOn(
                            innerDeclaration.source,
                            FirErrors.INNER_CLASS_INSIDE_VALUE_CLASS,
                            valueModifierPrefix,
                        )
                    }
                }

                is FirPropertySymbol -> {
                    if (innerDeclaration.isRelatedToParameter(primaryConstructorParametersByName[innerDeclaration.name])) {
                        primaryConstructorPropertiesByName[innerDeclaration.name] = innerDeclaration
                    } else {
                        when {
                            innerDeclaration.delegate != null ->
                                reporter.reportOn(
                                    innerDeclaration.delegate!!.source,
                                    FirErrors.DELEGATED_PROPERTY_INSIDE_VALUE_CLASS
                                )

                            innerDeclaration.hasBackingField &&
                                    innerDeclaration.source?.kind !is KtFakeSourceElementKind -> {
                                reporter.reportOn(
                                    innerDeclaration.source, FirErrors.PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS
                                )
                            }
                        }
                    }
                }

                else -> {}
            }
        }
        // Separate handling of delegate fields
        @OptIn(DirectDeclarationsAccess::class)
        declaration.declarations.forEach { innerDeclaration ->
            if (innerDeclaration !is FirField || !innerDeclaration.isSynthetic) return@forEach
            val symbol = innerDeclaration.initializer?.toResolvedCallableSymbol(context.session)
            if (symbol != null && symbol in primaryConstructorParametersSymbolsSet) {
                return@forEach
            }
            val delegatedTypeRefSource = (innerDeclaration.returnTypeRef as FirResolvedTypeRef).delegatedTypeRef?.source
            reporter.reportOn(
                delegatedTypeRefSource,
                FirErrors.VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION
            )
        }

        val reservedNames = when {
            isFullValueClass -> emptySet()
            isCustomEqualsSupported -> boxAndUnboxNames
            else -> boxAndUnboxNames + equalsAndHashCodeNames
        }
        val classScope = declaration.unsubstitutedScope()
        for (reservedName in reservedNames) {
            classScope.processFunctionsByName(Name.identifier(reservedName)) {
                val functionSymbol = it.unwrapFakeOverrides()
                if (functionSymbol.isAbstract) return@processFunctionsByName
                val containingClassSymbol = functionSymbol.getContainingClassSymbol() ?: return@processFunctionsByName
                if (containingClassSymbol == declaration.symbol) {
                    if (functionSymbol.source?.kind is KtRealSourceElementKind) {
                        reporter.reportOn(
                            functionSymbol.source,
                            FirErrors.RESERVED_MEMBER_INSIDE_VALUE_CLASS,
                            reservedName,
                        )
                    }
                } else if (containingClassSymbol.classKind == ClassKind.INTERFACE) {
                    reporter.reportOn(
                        declaration.source,
                        FirErrors.RESERVED_MEMBER_FROM_INTERFACE_INSIDE_VALUE_CLASS,
                        containingClassSymbol.name.asString(),
                        reservedName,
                    )
                }
            }
        }

        val finalOrBasicValueClassPrefix = when {
            !supportsFullValueClasses -> "value"
            isFullValueClass -> "final value"
            else -> "@JvmInline value"
        }
        if (primaryConstructor?.source?.kind is KtRealSourceElementKind) {
            if (isFullValueClass) {
                if (primaryConstructorParametersByName.isEmpty() && (!isFullValueClass || declaration.isFinal)) {
                    reporter.reportOn(
                        primaryConstructor.source,
                        FirErrors.VALUE_CLASS_EMPTY_CONSTRUCTOR,
                        finalOrBasicValueClassPrefix.capitalizeAsciiOnly(),
                    )
                    return
                }
            } else if (primaryConstructorParametersByName.size != 1) {
                val jvmInlineAnnotation = context.session.annotationPlatformSupport.jvmInlineAnnotationClassId
                val hasJvmInlineAnnotation = jvmInlineAnnotation != null && declaration.hasAnnotation(jvmInlineAnnotation, context.session)
                val valueModifier = declaration.getModifier(VALUE_KEYWORD)
                if (primaryConstructorParametersByName.size > 1 && !hasJvmInlineAnnotation && valueModifier != null) {
                    reporter.reportOn(
                        valueModifier.source, FirErrors.UNSUPPORTED_FEATURE, LanguageFeature.FullValueClasses to context.languageVersionSettings
                    )
                } else {
                    reporter.reportOn(
                        primaryConstructor.source, FirErrors.INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE, valueModifierPrefix
                    )
                }
                return
            }
        } else if (!isFullValueClass || declaration.isFinal) {
            if (!declaration.isExpect || LanguageFeature.AllowExpectValueClassesWithNoPrimaryConstructor.isDisabled()) {
                reporter.reportOn(
                    declaration.source, FirErrors.ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS, finalOrBasicValueClassPrefix
                )
            } else {
                declaration.constructors(context.session).filter { !it.isPrimary }.forEach { constructor ->
                    reporter.reportOn(
                        constructor.source,
                        FirErrors.EXPECT_VALUE_CLASS_WITH_NO_PRIMARY_CONSTRUCTOR_HAS_SECONDARY,
                        finalOrBasicValueClassPrefix,
                    )
                }
            }
            return
        }

        for ([name, primaryConstructorParameter] in primaryConstructorParametersByName) {
            val parameterTypeRef = primaryConstructorParameter.resolvedReturnTypeRef
            val recursionType = parameterTypeRef.coneType.getValueClassTypeRecursionType(context.session)
            when {
                declaration.isFinal && primaryConstructorParameter.isNotFinalReadOnly(primaryConstructorPropertiesByName[name]) ->
                    reporter.reportOn(
                        primaryConstructorParameter.source,
                        FirErrors.VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER,
                        if (supportsFullValueClasses) "Final value" else "Value",
                    )
                isFullValueClass && declaration.isAbstract && primaryConstructorPropertiesByName[name] != null -> reporter.reportOn(
                    primaryConstructorParameter.source,
                    FirErrors.ABSTRACT_VALUE_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER
                )

                isFullValueClass && declaration.isSealed && primaryConstructorPropertiesByName[name] != null -> reporter.reportOn(
                    primaryConstructorParameter.source,
                    FirErrors.SEALED_VALUE_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER
                )

                !isFullValueClass && parameterTypeRef.isInapplicableParameterType(context.session) -> {
                    reporter.reportOn(
                        parameterTypeRef.source,
                        FirErrors.VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE,
                        parameterTypeRef.coneType,
                        valueModifierPrefix,
                    )
                }

                // Currently, it is decided to forbid recursive value classes even for full value classes because they require implementation on non-JVM platforms.
                // As they can be expressed in Java, the prohibition creates a subtle difference between Java's and Kotlin's recursive unconstructible value classes.
                // Also, there appears a minor inconsistency between unconstructible value and non-value classes in Kotlin.
                // Neither of the issues affects any meaningful code.
                recursionType != null -> when (recursionType) {
                    Plain -> reporter.reportOn(parameterTypeRef.source, FirErrors.VALUE_CLASS_CANNOT_BE_RECURSIVE)
                    ViaTypeParameters -> reporter.reportOn(
                        parameterTypeRef.source, FirErrors.VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS,
                    )
                }
            }
        }

        if (isCustomEqualsSupported) {
            val [equalsFromAnyOverriding, typedEquals] = run {
                var equalsFromAnyOverriding: FirNamedFunctionSymbol? = null
                var typedEquals: FirNamedFunctionSymbol? = null
                declaration.processAllDeclarations(context.session) {
                    if (it !is FirNamedFunctionSymbol) {
                        return@processAllDeclarations
                    }
                    if (it.isEquals(context.session)) equalsFromAnyOverriding = it
                    if (it.isTypedEqualsInValueClass(context.session)) typedEquals = it
                }
                equalsFromAnyOverriding to typedEquals
            }
            if (typedEquals != null) {
                if (typedEquals.typeParameterSymbols.isNotEmpty()) {
                    reporter.reportOn(
                        typedEquals.source,
                        FirErrors.TYPE_PARAMETERS_NOT_ALLOWED
                    )
                }
                val singleParameterReturnTypeRef = typedEquals.valueParameterSymbols.single().resolvedReturnTypeRef
                if (singleParameterReturnTypeRef.coneType.typeArguments.any { !it.isStarProjection }) {
                    reporter.reportOn(singleParameterReturnTypeRef.source, FirErrors.TYPE_ARGUMENT_ON_TYPED_VALUE_CLASS_EQUALS)
                }
            }

            if (equalsFromAnyOverriding != null && typedEquals == null) {
                reporter.reportOn(
                    equalsFromAnyOverriding.source,
                    FirErrors.INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS,
                    declaration.defaultType().replaceArgumentsWithStarProjections()
                )
            }
        }
    }

    private fun FirPropertySymbol.isRelatedToParameter(parameter: FirValueParameterSymbol?) =
        name == parameter?.name && source?.kind is KtFakeSourceElementKind

    private fun FirValueParameterSymbol.isNotFinalReadOnly(primaryConstructorProperty: FirPropertySymbol?): Boolean {
        if (primaryConstructorProperty == null) return true

        val isOpen = hasModifier(KtTokens.OPEN_KEYWORD)

        return isVararg || !primaryConstructorProperty.isVal || isOpen
    }

    private fun FirTypeRef.isInapplicableParameterType(session: FirSession): Boolean =
        coneType.fullyExpandedType(session).let { it.isUnit || it.isNothing }

    private fun ConeKotlinType.isGenericArrayOfTypeParameter(): Boolean {
        if (this.typeArguments.firstOrNull() is ConeStarProjection || !isArrayOrPrimitiveArray())
            return false

        val arrayElementType = arrayElementType() ?: return false
        return arrayElementType is ConeTypeParameterType ||
                arrayElementType.isGenericArrayOfTypeParameter()
    }

    private fun FirRegularClass.isSubtypeOfCloneable(session: FirSession): Boolean {
        if (classId.isCloneableId()) return true

        return lookupSuperTypes(this, lookupInterfaces = true, deep = true, session, substituteTypes = false).any { superType ->
            // Note: We check just classId here, so type substitution isn't needed   ^ (we aren't interested in type arguments)
            superType.fullyExpandedType(session).lookupTag.classId.isCloneableId()
        }
    }

    private fun ClassId.isCloneableId(): Boolean =
        relativeClassName == cloneableFqName &&
                (packageFqName == StandardClassIds.BASE_KOTLIN_PACKAGE || packageFqName == javaLangFqName)

    private fun ClassId.isRecordId(): Boolean =
        relativeClassName == recordFqName && packageFqName == javaLangFqName
}
