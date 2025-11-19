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
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDestructuringDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInAnnotationCallChecker.getSubclassOptInApplicabilityAndMessage
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCodeFragmentSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.resolve.checkers.OptInInheritanceDiagnosticMessageProvider
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.checkers.OptInUsagesDiagnosticMessageProvider
import org.jetbrains.kotlin.resolve.checkers.OptInNames.OPT_IN_ANNOTATION_CLASS
import org.jetbrains.kotlin.resolve.checkers.OptInUsagesDiagnosticMessageProviderForDeprecation
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
    ): Experimentality? {
        return loadExperimentalityForMarkerAnnotation(session, annotatedOwnerClassName = null)
    }

    context(context: CheckerContext)
    fun FirConstructorSymbol.loadExperimentalitiesFromConstructor(): Set<Experimentality> {
        val result = mutableSetOf<Experimentality>()
        loadExperimentalitiesFromAnnotationTo(context.session, result)
        return result
    }

    fun FirBasedSymbol<*>.loadExperimentalitiesFromAnnotationTo(session: FirSession, result: MutableCollection<Experimentality>) {
        loadExperimentalitiesFromAnnotationTo(session, result, fromSupertype = false)
    }

    private fun FirBasedSymbol<*>.loadExperimentalitiesFromAnnotationTo(
        session: FirSession,
        result: MutableCollection<Experimentality>,
        fromSupertype: Boolean,
    ) {
        for (annotation in resolvedAnnotationsWithArguments) {
            val annotationType = annotation.annotationTypeRef.coneType as? ConeClassLikeType ?: continue
            val className = when (this) {
                is FirRegularClassSymbol -> name.asString()
                is FirCallableSymbol<*> -> callableId?.className?.shortName()?.asString()
                else -> null
            }
            result.addIfNotNull(
                annotationType.lookupTag.toRegularClassSymbol(
                    session
                )?.loadExperimentalityForMarkerAnnotation(session, className)
            )
            if (fromSupertype) {
                if (annotationType.lookupTag.classId == OptInNames.SUBCLASS_OPT_IN_REQUIRED_CLASS_ID) {
                    val annotationClass = annotation.findArgumentByName(OPT_IN_ANNOTATION_CLASS) ?: continue
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

    context(context: CheckerContext)
    fun loadExperimentalitiesFromTypeArguments(
        typeArguments: List<FirTypeProjection>,
    ): Set<Experimentality> {
        if (typeArguments.isEmpty()) return emptySet()
        return loadExperimentalitiesFromConeArguments(typeArguments.map { it.toConeTypeProjection() })
    }

    context(context: CheckerContext)
    fun loadExperimentalitiesFromConeArguments(
        typeArguments: List<ConeTypeProjection>,
    ): Set<Experimentality> {
        if (typeArguments.isEmpty()) return emptySet()
        val result = SmartSet.create<Experimentality>()
        typeArguments.forEach {
            if (!it.isStarProjection) it.type?.addExperimentalities(result)
        }
        return result
    }

    /**
     * For ERROR experimentalities that are found via the companion object symbol, but not via its outer class symbol,
     *  we report deprecation warnings instead of errors in case LF is not enabled.
     *
     * @return a pair of experimentalities found via outer class symbol and experimentalities found via companion object symbol only.
     */
    context(context: CheckerContext)
    fun FirBasedSymbol<*>.loadExperimentalitiesForQualifier(
        companionObjectSymbol: FirClassLikeSymbol<*>?
    ): Pair<Set<Experimentality>, Set<Experimentality>> {
        val visited = mutableSetOf<FirBasedSymbol<*>>()
        val allExperimentalities =
            loadExperimentalities(
                knownExperimentalities = null,
                visited = visited,
                fromSetter = false,
                dispatchReceiverType = null,
                fromSupertype = false,
            )

        if (companionObjectSymbol == null) return allExperimentalities to emptySet()

        // copy here because loadExperimentalities mutates its first argument
        val hardExperimentalities = allExperimentalities.toCollection(SmartSet.create())

        companionObjectSymbol.loadExperimentalities(
            knownExperimentalities = allExperimentalities,
            visited = visited,
            fromSetter = false,
            dispatchReceiverType = null,
            fromSupertype = false,
        )

        return hardExperimentalities to allExperimentalities - hardExperimentalities
    }

    context(context: CheckerContext)
    fun FirBasedSymbol<*>.loadExperimentalities(
        fromSetter: Boolean, dispatchReceiverType: ConeKotlinType?,
    ): Set<Experimentality> {
        return loadExperimentalities(
            knownExperimentalities = null, visited = mutableSetOf(), fromSetter, dispatchReceiverType, fromSupertype = false
        )
    }

    context(context: CheckerContext)
    fun FirClassLikeSymbol<*>.loadExperimentalitiesFromSupertype(): Set<Experimentality> =
        loadExperimentalities(
            knownExperimentalities = null, visited = mutableSetOf(),
            fromSetter = false, dispatchReceiverType = null, fromSupertype = true
        )

    fun FirClassLikeSymbol<*>.isExperimentalMarker(session: FirSession): Boolean =
        this is FirRegularClassSymbol && hasAnnotationWithClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID, session)

    context(context: CheckerContext)
    private fun FirBasedSymbol<*>.loadExperimentalities(
        knownExperimentalities: SmartSet<Experimentality>?,
        visited: MutableSet<FirBasedSymbol<*>>,
        fromSetter: Boolean,
        dispatchReceiverType: ConeKotlinType?,
        fromSupertype: Boolean,
    ): SmartSet<Experimentality> {
        if (!visited.add(this)) return SmartSet.create()
        val result = knownExperimentalities ?: SmartSet.create()
        val session = context.session
        when (this) {
            is FirCallableSymbol<*> ->
                loadCallableSpecificExperimentalities(
                    this, visited, fromSetter, dispatchReceiverType, result
                )
            is FirClassLikeSymbol<*> ->
                loadClassLikeSpecificExperimentalities(this, visited, result)
            is FirAnonymousInitializerSymbol, is FirFileSymbol, is FirTypeParameterSymbol,
            is FirScriptSymbol, is FirReplSnippetSymbol, is FirCodeFragmentSymbol, is FirReceiverParameterSymbol,
                -> {
            }
        }

        loadExperimentalitiesFromAnnotationTo(session, result, fromSupertype)

        if (hasAnnotationWithClassId(OptInNames.WAS_EXPERIMENTAL_CLASS_ID, session)) {
            val accessibility = checkSinceKotlinVersionAccessibility()
            if (accessibility is FirSinceKotlinAccessibility.NotAccessibleButWasExperimental) {
                accessibility.markerClasses.forEach {
                    result.addIfNotNull(it.loadExperimentalityForMarkerAnnotation(session))
                }
            }
        }

        return result
    }

    context(context: CheckerContext)
    private fun FirCallableSymbol<*>.loadCallableSpecificExperimentalities(
        symbol: FirCallableSymbol<*>,
        visited: MutableSet<FirBasedSymbol<*>>,
        fromSetter: Boolean,
        dispatchReceiverType: ConeKotlinType?,
        result: SmartSet<Experimentality>,
    ) {
        val parentClassSymbol = containingClassLookupTag()?.toRegularClassSymbol()
        if (this is FirConstructorSymbol) {
            val ownerClassLikeSymbol = this.typeAliasConstructorInfo?.typeAliasSymbol ?: parentClassSymbol
            // For other callable we check dispatch receiver type instead
            ownerClassLikeSymbol?.loadExperimentalities(
                result, visited, fromSetter = false, dispatchReceiverType = null, fromSupertype = false
            )
        } else {
            resolvedReturnType.abbreviatedTypeOrSelf.addExperimentalities(result, visited)
            receiverParameterSymbol?.resolvedType?.abbreviatedTypeOrSelf.addExperimentalities(result, visited)
        }
        if (!symbol.isStatic) {
            dispatchReceiverType?.addExperimentalities(result, visited)
        }
        if (this is FirFunctionSymbol) {
            valueParameterSymbols.forEach {
                it.resolvedReturnType.abbreviatedTypeOrSelf.addExperimentalities(result, visited)
            }

            // Handling data class 'componentN' function
            if (parentClassSymbol?.isData == true && DataClassResolver.isComponentLike(this.callableId.callableName) && parentClassSymbol.classKind == ClassKind.CLASS) {
                val componentNIndex = DataClassResolver.getComponentIndex(this.callableId.callableName.identifier)
                val valueParameters = parentClassSymbol.primaryConstructorIfAny(context.session)?.valueParameterSymbols
                val valueParameter = valueParameters?.getOrNull(componentNIndex - 1)
                val properties = parentClassSymbol.declaredProperties(context.session)
                val property = properties.firstOrNull { it.name == valueParameter?.name }
                property?.loadExperimentalities(result, visited, fromSetter = false, dispatchReceiverType, fromSupertype = false)
            }
        }

        if (fromSetter && symbol is FirPropertySymbol) {
            symbol.setterSymbol?.loadExperimentalities(
                result, visited, fromSetter = false, dispatchReceiverType, fromSupertype = false
            )
        }
    }

    context(context: CheckerContext)
    private fun FirClassLikeSymbol<*>.loadClassLikeSpecificExperimentalities(
        symbol: FirBasedSymbol<*>,
        visited: MutableSet<FirBasedSymbol<*>>,
        result: SmartSet<Experimentality>,
    ) {
        when (this) {
            is FirRegularClassSymbol -> if (symbol is FirRegularClassSymbol) {
                val parentClassSymbol = symbol.outerClassSymbol()
                parentClassSymbol?.loadExperimentalities(
                    result, visited, fromSetter = false, dispatchReceiverType = null, fromSupertype = false
                )
            }

            is FirAnonymousObjectSymbol, is FirTypeAliasSymbol -> {
            }
        }
    }

    context(context: CheckerContext)
    private fun ConeKotlinType?.addExperimentalities(
        result: SmartSet<Experimentality>,
        visited: MutableSet<FirBasedSymbol<*>> = mutableSetOf(),
    ) {
        if (this !is ConeClassLikeType) return
        lookupTag.toSymbol()?.loadExperimentalities(
            result, visited, fromSetter = false, dispatchReceiverType = null, fromSupertype = false
        )
        fullyExpandedType().typeArguments.forEach {
            if (!it.isStarProjection) it.type?.addExperimentalities(result, visited)
        }
    }

    // Note: receiver is an OptIn marker class and parameter is an annotated member owner class / self class name
    private fun FirRegularClassSymbol.loadExperimentalityForMarkerAnnotation(
        session: FirSession,
        annotatedOwnerClassName: String? = null,
    ): Experimentality? {
        val experimental = getAnnotationWithResolvedArgumentsByClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID, session)
            ?: return null

        val levelArgument = experimental.findArgumentByName(LEVEL)

        val levelName = levelArgument?.extractEnumValueArgumentInfo()?.enumEntryName?.asString()

        val severity = Experimentality.Severity.entries.firstOrNull { it.name == levelName } ?: Experimentality.DEFAULT_SEVERITY
        val message = experimental.getStringArgument(MESSAGE, session)
        return Experimentality(classId, severity, message, annotatedOwnerClassName)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun reportNotAcceptedExperimentalities(
        experimentalities: Collection<Experimentality>,
        element: FirElement,
        source: KtSourceElement? = element.source,
        reportErrorsAsDeprecationWarnings: Boolean = false,
    ) {
        val isSubclassOptInApplicable = (context.containingDeclarations.lastOrNull() as? FirClassSymbol)
            ?.let { getSubclassOptInApplicabilityAndMessage(it).first }
            ?: false
        for ((annotationClassId, severity, message, _, fromSupertype) in experimentalities) {
            if (!isExperimentalityAcceptableInContext(annotationClassId, fromSupertype)) {
                val (diagnostic, messageProvider, verb) = when (severity) {
                    Experimentality.Severity.WARNING if fromSupertype -> Triple(
                        FirErrors.OPT_IN_TO_INHERITANCE,
                        OptInInheritanceDiagnosticMessageProvider(isSubclassOptInApplicable),
                        "should"
                    )
                    Experimentality.Severity.WARNING -> Triple(
                        FirErrors.OPT_IN_USAGE,
                        OptInUsagesDiagnosticMessageProvider,
                        "should"
                    )
                    Experimentality.Severity.ERROR if fromSupertype -> Triple(
                        FirErrors.OPT_IN_TO_INHERITANCE_ERROR,
                        OptInInheritanceDiagnosticMessageProvider(isSubclassOptInApplicable),
                        "must"
                    )
                    Experimentality.Severity.ERROR if reportErrorsAsDeprecationWarnings -> Triple(
                        FirErrors.OPT_IN_USAGE,
                        OptInUsagesDiagnosticMessageProviderForDeprecation,
                        "must"
                    )
                    Experimentality.Severity.ERROR -> Triple(
                        FirErrors.OPT_IN_USAGE_ERROR,
                        OptInUsagesDiagnosticMessageProvider,
                        "must"
                    )
                }

                val reportedMessage =
                    if (!message.isNullOrBlank()) messageProvider.buildCustomDiagnosticMessage(message) else messageProvider.buildDefaultDiagnosticMessage(
                        annotationClassId.asFqNameString(),
                        verb
                    )

                reporter.reportOn(source, diagnostic, annotationClassId, reportedMessage)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun reportNotAcceptedOverrideExperimentalities(
        experimentalities: Collection<Experimentality>,
        symbol: FirCallableSymbol<*>,
    ) {
        for ((annotationClassId, severity, markerMessage, supertypeName) in experimentalities) {
            if (!symbol.isExperimentalityAcceptable(annotationClassId, fromSupertype = false) &&
                !isExperimentalityAcceptableInContext(annotationClassId, fromSupertype = false)
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
                reporter.reportOn(symbol.source, diagnostic, annotationClassId, message)
            }
        }
    }

    fun FirAnnotationCall.getSourceForIsMarkerDiagnostic(argumentIndex: Int): KtSourceElement? {
        val markerArgumentsSources = this.getMarkerArgumentsSources()
        return markerArgumentsSources[argumentIndex]
    }

    context(context: CheckerContext)
    private fun isExperimentalityAcceptableInContext(
        annotationClassId: ClassId,
        fromSupertype: Boolean,
    ): Boolean {
        val languageVersionSettings = context.session.languageVersionSettings
        val fqNameAsString = annotationClassId.asFqNameString()
        if (fqNameAsString in languageVersionSettings.getFlag(AnalysisFlags.optIn)) {
            return true
        }
        for (annotationContainer in context.annotationContainers) {
            if (annotationContainer is FirDeclaration &&
                annotationContainer.symbol.isExperimentalityAcceptable(annotationClassId, fromSupertype)
            ) {
                return true
            }
            if (annotationContainer is FirStatement) {
                with(annotationContainer) {
                    if (getAnnotationByClassId(annotationClassId, context.session) != null ||
                        annotations.isAnnotatedWithOptIn(annotationClassId)
                    ) {
                        return true
                    }
                }
            }
        }

        val containingDeclaration = context.containingDeclarations.lastOrNull()
        if (containingDeclaration is FirPropertySymbol) {
            @OptIn(SymbolInternals::class)
            FirDestructuringDeclarationChecker.getDestructuringVariableIfEntry(containingDeclaration.fir)?.let {
                if (it.isExperimentalityAcceptable(annotationClassId, fromSupertype)) {
                    return true
                }
            }
        }

        return false
    }

    context(context: CheckerContext)
    private fun FirBasedSymbol<*>.isExperimentalityAcceptable(
        annotationClassId: ClassId,
        fromSupertype: Boolean,
    ): Boolean {
        return hasAnnotationWithClassId(annotationClassId, context.session) ||
                isAnnotatedWithOptIn(annotationClassId) ||
                fromSupertype && isAnnotatedWithSubclassOptInRequired(annotationClassId) ||
                // Technically wrong but required for K1 compatibility
                primaryConstructorParameterIsExperimentalityAcceptable(annotationClassId) ||
                isImplicitDeclaration()
    }

    private fun FirBasedSymbol<*>.isImplicitDeclaration(): Boolean {
        return this.origin != FirDeclarationOrigin.Source
    }

    context(context: CheckerContext)
    private fun FirBasedSymbol<*>.primaryConstructorParameterIsExperimentalityAcceptable(
        annotationClassId: ClassId,
    ): Boolean {
        if (this !is FirPropertySymbol) return false
        val parameterSymbol = correspondingValueParameterFromPrimaryConstructor ?: return false

        return parameterSymbol.isExperimentalityAcceptable(annotationClassId, fromSupertype = false)
    }

    context(context: CheckerContext)
    private fun FirBasedSymbol<*>.isAnnotatedWithOptIn(annotationClassId: ClassId): Boolean {
        return resolvedAnnotationsWithArguments.isAnnotatedWithOptIn(annotationClassId)
    }

    context(context: CheckerContext)
    private fun List<FirAnnotation>.isAnnotatedWithOptIn(annotationClassId: ClassId): Boolean {
        for (annotation in this) {
            val coneType = annotation.annotationTypeRef.coneType as? ConeClassLikeType
            if (coneType?.lookupTag?.classId != OptInNames.OPT_IN_CLASS_ID) {
                continue
            }
            val annotationClasses = annotation.findArgumentByName(OPT_IN_ANNOTATION_CLASS) ?: continue
            if (annotationClasses.extractClassesFromArgument(context.session).any { it.classId == annotationClassId }) {
                return true
            }
        }
        return false
    }

    context(context: CheckerContext)
    private fun FirBasedSymbol<*>.isAnnotatedWithSubclassOptInRequired(
        annotationClassId: ClassId,
    ): Boolean {
        for (annotation in resolvedAnnotationsWithArguments) {
            val coneType = annotation.annotationTypeRef.coneType as? ConeClassLikeType
            if (coneType?.lookupTag?.classId != OptInNames.SUBCLASS_OPT_IN_REQUIRED_CLASS_ID) {
                continue
            }
            val annotationClass = annotation.findArgumentByName(OPT_IN_ANNOTATION_CLASS) ?: continue
            if (annotationClass.extractClassesFromArgument(context.session).any { it.classId == annotationClassId }) {
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
