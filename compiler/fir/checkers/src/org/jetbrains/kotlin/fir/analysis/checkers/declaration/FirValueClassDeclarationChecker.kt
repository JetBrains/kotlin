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
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitAnyTypeRef
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

sealed class FirValueClassDeclarationChecker(mppKind: MppCheckerKind) : FirRegularClassChecker(mppKind) {
    object Regular : FirValueClassDeclarationChecker(MppCheckerKind.Platform) {
        override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    object ForExpectClass : FirValueClassDeclarationChecker(MppCheckerKind.Common) {
        override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (!declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    companion object {
        private val boxAndUnboxNames = setOf("box", "unbox")
        private val equalsAndHashCodeNames = setOf("equals", "hashCode")
        private val javaLangFqName = FqName("java.lang")
        private val cloneableFqName = FqName("Cloneable")
    }

    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.symbol.isInlineOrValueClass()) {
            return
        }

        if (declaration.isInner || declaration.isLocal) {
            reporter.reportOn(declaration.source, FirErrors.VALUE_CLASS_NOT_TOP_LEVEL, context)
        }

        if (declaration.modality != Modality.FINAL) {
            reporter.reportOn(declaration.source, FirErrors.VALUE_CLASS_NOT_FINAL, context)
        }

        if (declaration.contextParameters.isNotEmpty() && context.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) {
            reporter.reportOn(declaration.source, FirErrors.VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS, context)
        }


        for (supertypeEntry in declaration.superTypeRefs) {
            if (supertypeEntry is FirImplicitAnyTypeRef || supertypeEntry is FirErrorTypeRef) continue
            if (supertypeEntry.toRegularClassSymbol(context.session)?.isInterface == true) continue
            reporter.reportOn(supertypeEntry.source, FirErrors.VALUE_CLASS_CANNOT_EXTEND_CLASSES, context)
        }

        if (declaration.isSubtypeOfCloneable(context.session)) {
            reporter.reportOn(declaration.source, FirErrors.VALUE_CLASS_CANNOT_BE_CLONEABLE, context)
        }

        var primaryConstructor: FirConstructorSymbol? = null
        var primaryConstructorParametersByName = mapOf<Name, FirValueParameterSymbol>()
        val primaryConstructorPropertiesByName = mutableMapOf<Name, FirPropertySymbol>()
        var primaryConstructorParametersSymbolsSet = setOf<FirValueParameterSymbol>()
        val isCustomEqualsSupported = context.languageVersionSettings.supportsFeature(LanguageFeature.CustomEqualsInValueClasses)

        declaration.constructors(context.session).forEach { innerDeclaration ->
            when {
                innerDeclaration.isPrimary -> {
                    primaryConstructor = innerDeclaration
                    primaryConstructorParametersByName = innerDeclaration.valueParameterSymbols.associateBy { it.name }
                    primaryConstructorParametersSymbolsSet = primaryConstructorParametersByName.values.toSet()
                }

                innerDeclaration.hasBody && !context.languageVersionSettings.supportsFeature(
                    LanguageFeature.ValueClassesSecondaryConstructorWithBody
                ) -> {
                    reporter.reportOn(
                        innerDeclaration.bodySource!!, FirErrors.SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS, context
                    )
                }
            }
        }
        declaration.processAllDeclarations(context.session) { innerDeclaration ->
            when (innerDeclaration) {
                is FirRegularClassSymbol -> {
                    if (innerDeclaration.isInner) {
                        reporter.reportOn(innerDeclaration.source, FirErrors.INNER_CLASS_INSIDE_VALUE_CLASS, context)
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
        // Separate handling of delegate fields
        @OptIn(DirectDeclarationsAccess::class)
        declaration.declarations.forEach { innerDeclaration ->
            if (innerDeclaration !is FirField || !innerDeclaration.isSynthetic) return@forEach
            val symbol = innerDeclaration.initializer?.toResolvedCallableSymbol(context.session)
            if (context.languageVersionSettings.supportsFeature(LanguageFeature.InlineClassImplementationByDelegation) &&
                symbol != null && symbol in primaryConstructorParametersSymbolsSet
            ) {
                return@forEach
            }
            val delegatedTypeRefSource = (innerDeclaration.returnTypeRef as FirResolvedTypeRef).delegatedTypeRef?.source
            reporter.reportOn(
                delegatedTypeRefSource,
                FirErrors.VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION,
                context
            )
        }

        val reservedNames = boxAndUnboxNames + if (isCustomEqualsSupported) emptySet() else equalsAndHashCodeNames
        val classScope = declaration.unsubstitutedScope(context)
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
                            context
                        )
                    }
                } else if (containingClassSymbol.classKind == ClassKind.INTERFACE) {
                    reporter.reportOn(
                        declaration.source,
                        FirErrors.RESERVED_MEMBER_FROM_INTERFACE_INSIDE_VALUE_CLASS,
                        containingClassSymbol.name.asString(),
                        reservedName,
                        context
                    )
                }
            }
        }

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

        for ((name, primaryConstructorParameter) in primaryConstructorParametersByName) {
            val parameterTypeRef = primaryConstructorParameter.resolvedReturnTypeRef
            when {
                primaryConstructorParameter.isNotFinalReadOnly(primaryConstructorPropertiesByName[name]) ->
                    reporter.reportOn(
                        primaryConstructorParameter.source,
                        FirErrors.VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER,
                        context
                    )

                !context.languageVersionSettings.supportsFeature(LanguageFeature.GenericInlineClassParameter) &&
                        parameterTypeRef.coneType.let {
                            it is ConeTypeParameterType || it.isGenericArrayOfTypeParameter()
                        } -> {
                    reporter.reportOn(
                        parameterTypeRef.source,
                        FirErrors.UNSUPPORTED_FEATURE,
                        LanguageFeature.GenericInlineClassParameter to context.languageVersionSettings,
                        context
                    )
                }

                parameterTypeRef.isInapplicableParameterType(context.session) -> {
                    reporter.reportOn(
                        parameterTypeRef.source,
                        FirErrors.VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE,
                        parameterTypeRef.coneType,
                        context
                    )
                }

                parameterTypeRef.coneType.isRecursiveValueClassType(context.session) -> {
                    reporter.reportOn(
                        parameterTypeRef.source, FirErrors.VALUE_CLASS_CANNOT_BE_RECURSIVE,
                        context
                    )
                }

                declaration.multiFieldValueClassRepresentation != null -> {
                    val defaultValue = primaryConstructorParameter.resolvedDefaultValue
                    if (defaultValue != null) {
                        // TODO, KT-50113: Fix when inline arguments are supported.
                        reporter.reportOn(
                            defaultValue.source,
                            FirErrors.MULTI_FIELD_VALUE_CLASS_PRIMARY_CONSTRUCTOR_DEFAULT_PARAMETER,
                            context
                        )
                    }
                }
            }
        }

        if (isCustomEqualsSupported) {
            val (equalsFromAnyOverriding, typedEquals) = run {
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
                        FirErrors.TYPE_PARAMETERS_NOT_ALLOWED,
                        context
                    )
                }
                val singleParameterReturnTypeRef = typedEquals.valueParameterSymbols.single().resolvedReturnTypeRef
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
        if (this.typeArguments.firstOrNull() is ConeStarProjection || !isPotentiallyArray())
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
}
