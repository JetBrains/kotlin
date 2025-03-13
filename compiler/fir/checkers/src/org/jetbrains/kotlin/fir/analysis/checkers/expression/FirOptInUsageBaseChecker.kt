/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInAnnotationCallChecker.getSubclassOptInApplicabilityAndMessage
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.resolve.checkers.OptInInheritanceDiagnosticMessageProvider
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.checkers.OptInUsagesDiagnosticMessageProvider
import org.jetbrains.kotlin.resolve.checkers.OptInNames.OPT_IN_ANNOTATION_CLASS
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addIfNotNull

object FirOptInUsageBaseChecker {
    data class Experimentality(
        val annotationClassId: ClassId,
        val severity: Severity,
        val message: String?,
        val supertypeName: String? = null,
        val fromSupertype: Boolean = false,
    ) {
        enum class Severity { WARNING, ERROR }
        companion object {
            val DEFAULT_SEVERITY: Severity = Severity.ERROR
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Experimentality) return false

            if (annotationClassId != other.annotationClassId) return false
            if (severity != other.severity) return false
            if (message != other.message) return false
            if (fromSupertype != other.fromSupertype) return false

            return true
        }

        override fun hashCode(): Int {
            var result = annotationClassId.hashCode()
            result = 31 * result + severity.hashCode()
            result = 31 * result + (message?.hashCode() ?: 0)
            result = 31 * result + fromSupertype.hashCode()
            return result
        }
    }

    // Note: receiver is an OptIn marker class and parameter is an annotated member owner class / self class name
    fun FirRegularClassSymbol.loadExperimentalityForMarkerAnnotation(
        session: FirSession,
        annotatedOwnerClassName: String? = null,
    ): Experimentality? {
        lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        @OptIn(SymbolInternals::class)
        return fir.loadExperimentalityForMarkerAnnotation(session, annotatedOwnerClassName)
    }

    fun FirBasedSymbol<*>.loadExperimentalitiesFromAnnotationTo(session: FirSession, result: MutableCollection<Experimentality>) {
        lazyResolveToPhase(FirResolvePhase.STATUS)
        @OptIn(SymbolInternals::class)
        fir.loadExperimentalitiesFromAnnotationTo(session, result, fromSupertype = false)
    }

    private fun FirDeclaration.loadExperimentalitiesFromAnnotationTo(
        session: FirSession,
        result: MutableCollection<Experimentality>,
        fromSupertype: Boolean,
    ) {
        for (annotation in annotations) {
            val annotationType = annotation.annotationTypeRef.coneType as? ConeClassLikeType ?: continue
            val className = when (this) {
                is FirRegularClass -> name.asString()
                is FirCallableDeclaration -> symbol.callableId.className?.shortName()?.asString()
                else -> null
            }
            result.addIfNotNull(
                annotationType.lookupTag.toRegularClassSymbol(
                    session
                )?.loadExperimentalityForMarkerAnnotation(session, className)
            )
            if (fromSupertype) {
                if (annotationType.lookupTag.classId == OptInNames.SUBCLASS_OPT_IN_REQUIRED_CLASS_ID) {
                    val annotationClass = annotation.findArgumentByName(OptInNames.OPT_IN_ANNOTATION_CLASS) ?: continue
                    val classes = annotationClass.extractClassesFromArgument(session)
                    classes.forEach { klass ->
                        result.addIfNotNull(
                            klass.loadExperimentalityForMarkerAnnotation(session)?.copy(fromSupertype = true)
                        )
                    }
                }
            }
        }
    }

    fun loadExperimentalitiesFromTypeArguments(
        context: CheckerContext,
        typeArguments: List<FirTypeProjection>,
    ): Set<Experimentality> {
        if (typeArguments.isEmpty()) return emptySet()
        return loadExperimentalitiesFromConeArguments(context, typeArguments.map { it.toConeTypeProjection() })
    }

    fun loadExperimentalitiesFromConeArguments(
        context: CheckerContext,
        typeArguments: List<ConeTypeProjection>,
    ): Set<Experimentality> {
        if (typeArguments.isEmpty()) return emptySet()
        val result = SmartSet.create<Experimentality>()
        typeArguments.forEach {
            if (!it.isStarProjection) it.type?.addExperimentalities(context, result)
        }
        return result
    }

    fun FirBasedSymbol<*>.loadExperimentalities(
        context: CheckerContext, fromSetter: Boolean, dispatchReceiverType: ConeKotlinType?,
    ): Set<Experimentality> = loadExperimentalities(
        context, knownExperimentalities = null, visited = mutableSetOf(), fromSetter, dispatchReceiverType, fromSupertype = false
    )

    fun FirClassLikeSymbol<*>.loadExperimentalitiesFromSupertype(context: CheckerContext): Set<Experimentality> = loadExperimentalities(
        context, knownExperimentalities = null, visited = mutableSetOf(),
        fromSetter = false, dispatchReceiverType = null, fromSupertype = true
    )

    fun FirClassLikeSymbol<*>.isExperimentalMarker(session: FirSession) =
        this is FirRegularClassSymbol && getAnnotationByClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID, session) != null

    @OptIn(SymbolInternals::class)
    private fun FirBasedSymbol<*>.loadExperimentalities(
        context: CheckerContext,
        knownExperimentalities: SmartSet<Experimentality>?,
        visited: MutableSet<FirDeclaration>,
        fromSetter: Boolean,
        dispatchReceiverType: ConeKotlinType?,
        fromSupertype: Boolean,
    ): Set<Experimentality> {
        lazyResolveToPhase(FirResolvePhase.STATUS)
        val fir = this.fir
        if (!visited.add(fir)) return emptySet()
        val result = knownExperimentalities ?: SmartSet.create()
        val session = context.session
        when (fir) {
            is FirCallableDeclaration ->
                fir.loadCallableSpecificExperimentalities(
                    this as FirCallableSymbol, context, visited, fromSetter, dispatchReceiverType, result
                )
            is FirClassLikeDeclaration ->
                fir.loadClassLikeSpecificExperimentalities(this, context, visited, result)
            is FirAnonymousInitializer, is FirDanglingModifierList, is FirFile, is FirTypeParameter,
            is FirScript, is FirReplSnippet, is FirCodeFragment, is FirReceiverParameter,
                -> {}
        }

        lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)
        fir.loadExperimentalitiesFromAnnotationTo(session, result, fromSupertype)

        if (fir.getAnnotationByClassId(OptInNames.WAS_EXPERIMENTAL_CLASS_ID, session) != null) {
            val accessibility = fir.checkSinceKotlinVersionAccessibility(context)
            if (accessibility is FirSinceKotlinAccessibility.NotAccessibleButWasExperimental) {
                accessibility.markerClasses.forEach {
                    it.lazyResolveToPhase(FirResolvePhase.STATUS)
                    result.addIfNotNull(it.fir.loadExperimentalityForMarkerAnnotation(session))
                }
            }
        }

        return result
    }

    private fun FirCallableDeclaration.loadCallableSpecificExperimentalities(
        symbol: FirCallableSymbol<*>,
        context: CheckerContext,
        visited: MutableSet<FirDeclaration>,
        fromSetter: Boolean,
        dispatchReceiverType: ConeKotlinType?,
        result: SmartSet<Experimentality>,
    ) {
        val parentClassSymbol = containingClassLookupTag()?.toRegularClassSymbol(context.session)
        if (this is FirConstructor) {
            val ownerClassLikeSymbol = this.typeAliasConstructorInfo?.typeAliasSymbol ?: parentClassSymbol
            // For other callable we check dispatch receiver type instead
            ownerClassLikeSymbol?.loadExperimentalities(
                context, result, visited, fromSetter = false, dispatchReceiverType = null, fromSupertype = false
            )
        } else {
            returnTypeRef.coneType.abbreviatedTypeOrSelf.addExperimentalities(context, result, visited)
            receiverParameter?.typeRef?.coneType?.abbreviatedTypeOrSelf.addExperimentalities(context, result, visited)
        }
        if (!symbol.isStatic) {
            dispatchReceiverType?.addExperimentalities(context, result, visited)
        }
        if (this is FirFunction) {
            valueParameters.forEach {
                it.returnTypeRef.coneType.abbreviatedTypeOrSelf.addExperimentalities(context, result, visited)
            }

            // Handling data class 'componentN' function
            if (parentClassSymbol?.isData == true && DataClassResolver.isComponentLike(this.nameOrSpecialName) && parentClassSymbol.classKind == ClassKind.CLASS) {
                val componentNIndex = DataClassResolver.getComponentIndex(this.nameOrSpecialName.identifier)
                val valueParameters = parentClassSymbol.primaryConstructorIfAny(context.session)?.valueParameterSymbols
                val valueParameter = valueParameters?.getOrNull(componentNIndex - 1)
                val properties = parentClassSymbol.declaredProperties(context.session)
                val property = properties.firstOrNull { it.name == valueParameter?.name }
                property?.loadExperimentalities(context, result, visited, fromSetter = false, dispatchReceiverType, fromSupertype = false)
            }
        }

        if (fromSetter && symbol is FirPropertySymbol) {
            symbol.setterSymbol?.loadExperimentalities(
                context, result, visited, fromSetter = false, dispatchReceiverType, fromSupertype = false
            )
        }
    }

    private fun FirClassLikeDeclaration.loadClassLikeSpecificExperimentalities(
        symbol: FirBasedSymbol<*>,
        context: CheckerContext,
        visited: MutableSet<FirDeclaration>,
        result: SmartSet<Experimentality>,
    ) {
        when (this) {
            is FirRegularClass -> if (symbol is FirRegularClassSymbol) {
                val parentClassSymbol = symbol.outerClassSymbol(context)
                parentClassSymbol?.loadExperimentalities(
                    context, result, visited, fromSetter = false, dispatchReceiverType = null, fromSupertype = false
                )
            }

            is FirAnonymousObject, is FirTypeAlias -> {
            }
        }
    }

    private fun ConeKotlinType?.addExperimentalities(
        context: CheckerContext,
        result: SmartSet<Experimentality>,
        visited: MutableSet<FirDeclaration> = mutableSetOf(),
    ) {
        if (this !is ConeClassLikeType) return
        lookupTag.toSymbol(context.session)?.loadExperimentalities(
            context, result, visited, fromSetter = false, dispatchReceiverType = null, fromSupertype = false
        )
        fullyExpandedType(context.session).typeArguments.forEach {
            if (!it.isStarProjection) it.type?.addExperimentalities(context, result, visited)
        }
    }

    // Note: receiver is an OptIn marker class and parameter is an annotated member owner class / self class name
    private fun FirRegularClass.loadExperimentalityForMarkerAnnotation(
        session: FirSession,
        annotatedOwnerClassName: String? = null,
    ): Experimentality? {
        val experimental = getAnnotationByClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID, session)
            ?: return null

        val levelArgument = experimental.findArgumentByName(LEVEL)

        val levelName = levelArgument?.extractEnumValueArgumentInfo()?.enumEntryName?.asString()

        val severity = Experimentality.Severity.entries.firstOrNull { it.name == levelName } ?: Experimentality.DEFAULT_SEVERITY
        val message = (experimental.findArgumentByName(MESSAGE) as? FirLiteralExpression)?.value as? String
        return Experimentality(symbol.classId, severity, message, annotatedOwnerClassName)
    }

    fun reportNotAcceptedExperimentalities(
        experimentalities: Collection<Experimentality>,
        element: FirElement,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        source: KtSourceElement? = element.source,
    ) {
        val isSubclassOptInApplicable =
            (context.containingDeclarations.lastOrNull() as? FirClass)?.let { getSubclassOptInApplicabilityAndMessage(it).first } ?: false
        for ((annotationClassId, severity, message, _, fromSupertype) in experimentalities) {
            if (!isExperimentalityAcceptableInContext(annotationClassId, context, fromSupertype)) {
                val (diagnostic, messageProvider, verb) = when {
                    fromSupertype && severity == Experimentality.Severity.WARNING -> Triple(
                        FirErrors.OPT_IN_TO_INHERITANCE,
                        OptInInheritanceDiagnosticMessageProvider(isSubclassOptInApplicable),
                        "should"
                    )
                    severity == Experimentality.Severity.WARNING -> Triple(
                        FirErrors.OPT_IN_USAGE,
                        OptInUsagesDiagnosticMessageProvider,
                        "should"
                    )
                    fromSupertype && severity == Experimentality.Severity.ERROR -> Triple(
                        FirErrors.OPT_IN_TO_INHERITANCE_ERROR,
                        OptInInheritanceDiagnosticMessageProvider(isSubclassOptInApplicable),
                        "must"
                    )
                    severity == Experimentality.Severity.ERROR -> Triple(
                        FirErrors.OPT_IN_USAGE_ERROR,
                        OptInUsagesDiagnosticMessageProvider,
                        "must"
                    )
                    else -> error("Unexpected $severity type")
                }

                val reportedMessage =
                    if (!message.isNullOrBlank()) messageProvider.buildCustomDiagnosticMessage(message) else messageProvider.buildDefaultDiagnosticMessage(
                        annotationClassId.asFqNameString(),
                        verb
                    )

                reporter.reportOn(source, diagnostic, annotationClassId, reportedMessage, context)
            }
        }
    }

    @SymbolInternals
    fun reportNotAcceptedOverrideExperimentalities(
        experimentalities: Collection<Experimentality>,
        symbol: FirCallableSymbol<*>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        for ((annotationClassId, severity, markerMessage, supertypeName) in experimentalities) {
            if (!symbol.fir.isExperimentalityAcceptable(context.session, annotationClassId, fromSupertype = false) &&
                !isExperimentalityAcceptableInContext(annotationClassId, context, fromSupertype = false)
            ) {
                val (diagnostic, verb) = when (severity) {
                    Experimentality.Severity.WARNING -> FirErrors.OPT_IN_OVERRIDE to "should"
                    Experimentality.Severity.ERROR -> FirErrors.OPT_IN_OVERRIDE_ERROR to "must"
                }
                val message = OptInNames.buildOverrideMessage(
                    supertypeName ?: "???",
                    markerMessage,
                    verb,
                    markerName = annotationClassId.asFqNameString()
                )
                reporter.reportOn(symbol.source, diagnostic, annotationClassId, message, context)
            }
        }
    }

    fun FirAnnotationCall.getSourceForIsMarkerDiagnostic(argumentIndex: Int): KtSourceElement? {
        val markerArgumentsSources = this.getMarkerArgumentsSources()
        return markerArgumentsSources[argumentIndex]
    }

    private fun isExperimentalityAcceptableInContext(
        annotationClassId: ClassId,
        context: CheckerContext,
        fromSupertype: Boolean,
    ): Boolean {
        val languageVersionSettings = context.session.languageVersionSettings
        val fqNameAsString = annotationClassId.asFqNameString()
        if (fqNameAsString in languageVersionSettings.getFlag(AnalysisFlags.optIn)) {
            return true
        }
        for (annotationContainer in context.annotationContainers) {
            if (annotationContainer.isExperimentalityAcceptable(context.session, annotationClassId, fromSupertype)) {
                return true
            }
        }
        return false
    }

    private fun FirAnnotationContainer.isExperimentalityAcceptable(
        session: FirSession,
        annotationClassId: ClassId,
        fromSupertype: Boolean,
    ): Boolean {
        return getAnnotationByClassId(annotationClassId, session) != null ||
                isAnnotatedWithOptIn(annotationClassId, session) ||
                fromSupertype && isAnnotatedWithSubclassOptInRequired(session, annotationClassId) ||
                // Technically wrong but required for K1 compatibility
                primaryConstructorParameterIsExperimentalityAcceptable(session, annotationClassId) ||
                isImplicitDeclaration()
    }

    private fun FirAnnotationContainer.isImplicitDeclaration(): Boolean {
        return this is FirDeclaration && this.origin != FirDeclarationOrigin.Source
    }

    @OptIn(SymbolInternals::class)
    private fun FirAnnotationContainer.primaryConstructorParameterIsExperimentalityAcceptable(
        session: FirSession,
        annotationClassId: ClassId,
    ): Boolean {
        if (this !is FirProperty) return false
        val parameterSymbol = correspondingValueParameterFromPrimaryConstructor ?: return false

        return parameterSymbol.fir.isExperimentalityAcceptable(session, annotationClassId, fromSupertype = false)
    }

    private fun FirAnnotationContainer.isAnnotatedWithOptIn(annotationClassId: ClassId, session: FirSession): Boolean {
        for (annotation in annotations) {
            val coneType = annotation.annotationTypeRef.coneType as? ConeClassLikeType
            if (coneType?.lookupTag?.classId != OptInNames.OPT_IN_CLASS_ID) {
                continue
            }
            val annotationClasses = annotation.findArgumentByName(OptInNames.OPT_IN_ANNOTATION_CLASS) ?: continue
            if (annotationClasses.extractClassesFromArgument(session).any { it.classId == annotationClassId }) {
                return true
            }
        }
        return false
    }

    private fun FirAnnotationContainer.isAnnotatedWithSubclassOptInRequired(
        session: FirSession,
        annotationClassId: ClassId,
    ): Boolean {
        for (annotation in annotations) {
            val coneType = annotation.annotationTypeRef.coneType as? ConeClassLikeType
            if (coneType?.lookupTag?.classId != OptInNames.SUBCLASS_OPT_IN_REQUIRED_CLASS_ID) {
                continue
            }
            val annotationClass = annotation.findArgumentByName(OptInNames.OPT_IN_ANNOTATION_CLASS) ?: continue
            if (annotationClass.extractClassesFromArgument(session).any { it.classId == annotationClassId }) {
                return true
            }
        }
        return false
    }

    private fun FirAnnotationCall.getMarkerArgumentsSources(): List<KtSourceElement?> {
        val annotationClasses = this.findArgumentByName(OPT_IN_ANNOTATION_CLASS)
        val markerArgumentsSources =
            if (annotationClasses is FirVarargArgumentsExpression) annotationClasses.arguments.map { it.source } else listOfNotNull(
                annotationClasses?.source
            )
        return markerArgumentsSources
    }

    private val LEVEL = Name.identifier("level")
    private val MESSAGE = Name.identifier("message")
}
