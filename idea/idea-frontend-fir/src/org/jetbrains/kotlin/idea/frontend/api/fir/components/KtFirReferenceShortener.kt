/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.impl.source.PostprocessReformattingAspect
import com.intellij.psi.util.parentsOfType
import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.declarations.builder.buildResolvedImport
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.resolveToPackageOrClass
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirExplicitSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.LowLevelFirApiFacadeForResolveOnAir
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFir
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtReferenceShortener
import org.jetbrains.kotlin.idea.frontend.api.components.ShortenCommand
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.addImportToFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability

internal class KtFirReferenceShortener(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
    override val firResolveState: FirModuleResolveState,
) : KtReferenceShortener(), KtFirAnalysisSessionComponent {
    private val context = FirShorteningContext(firResolveState)

    override fun collectShortenings(file: KtFile, selection: TextRange): ShortenCommand {
        val declarationToVisit = file.findSmallestDeclarationContainingSelection(selection)
            ?: file.withDeclarationsResolvedToBodyResolve()

        val firDeclaration = declarationToVisit.getOrBuildFir(firResolveState)

        val towerContext =
            LowLevelFirApiFacadeForResolveOnAir.onAirGetTowerContextProvider(firResolveState, declarationToVisit)

        val collector = ElementsToShortenCollector(context, towerContext)
        firDeclaration.accept(collector)

        return ShortenCommandImpl(
            file,
            collector.namesToImport.distinct(),
            collector.typesToShorten.distinct().map { it.createSmartPointer() },
            collector.qualifiersToShorten.distinct().map { it.createSmartPointer() }
        )
    }

    private fun KtFile.withDeclarationsResolvedToBodyResolve(): KtFile {
        for (declaration in declarations) {
            declaration.getOrBuildFir(firResolveState) // temporary hack, resolves declaration to BODY_RESOLVE stage
        }

        return this
    }
}

private fun KtFile.findSmallestDeclarationContainingSelection(selection: TextRange): KtDeclaration? =
    findElementAt(selection.startOffset)
        ?.parentsOfType<KtDeclaration>(withSelf = true)
        ?.firstOrNull { selection in it.textRange }

private data class AvailableClassifier(val classId: ClassId, val isFromStarOrPackageImport: Boolean)

private class FirShorteningContext(val firResolveState: FirModuleResolveState) {

    private val firSession: FirSession
        get() = firResolveState.rootModuleSession

    fun findFirstClassifierInScopesByName(positionScopes: List<FirScope>, targetClassName: Name): AvailableClassifier? {
        for (scope in positionScopes) {
            val classifierSymbol = scope.findFirstClassifierByName(targetClassName) ?: continue
            val classifierLookupTag = classifierSymbol.toLookupTag() as? ConeClassLikeLookupTag ?: continue

            return AvailableClassifier(
                classifierLookupTag.classId,
                isFromStarOrPackageImport = scope is FirAbstractStarImportingScope || scope is FirPackageMemberScope
            )
        }

        return null
    }

    fun findFunctionsInScopes(scopes: List<FirScope>, name: Name): List<FirNamedFunctionSymbol> {
        return scopes.flatMap { it.getFunctions(name) }
    }

    fun findPropertiesInScopes(scopes: List<FirScope>, name: Name): List<FirVariableSymbol<*>> {
        return scopes.flatMap { it.getProperties(name) }
    }

    private fun FirScope.findFirstClassifierByName(name: Name): FirClassifierSymbol<*>? {
        var element: FirClassifierSymbol<*>? = null

        processClassifiersByName(name) {
            if (element == null) {
                element = it
            }
        }

        return element
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun findScopesAtPosition(
        position: KtElement,
        newImports: List<FqName>,
        towerContextProvider: FirTowerContextProvider
    ): List<FirScope>? {
        val towerDataContext = towerContextProvider.getClosestAvailableParentContext(position) ?: return null
        val result = buildList<FirScope> {
            addAll(towerDataContext.nonLocalTowerDataElements.mapNotNull { it.scope })
            addIfNotNull(createFakeImportingScope(newImports))
            addAll(towerDataContext.localScopes)
        }

        return result.asReversed()
    }

    private fun createFakeImportingScope(newImports: List<FqName>): FirScope? {
        val resolvedNewImports = newImports.mapNotNull { createFakeResolvedImport(it) }
        if (resolvedNewImports.isEmpty()) return null

        return FirExplicitSimpleImportingScope(resolvedNewImports, firSession, ScopeSession())
    }

    private fun createFakeResolvedImport(fqNameToImport: FqName): FirResolvedImport? {
        val packageOrClass = resolveToPackageOrClass(firSession.symbolProvider, fqNameToImport) ?: return null

        val delegateImport = buildImport {
            importedFqName = fqNameToImport
            isAllUnder = false
        }

        return buildResolvedImport {
            delegate = delegateImport
            packageFqName = packageOrClass.packageFqName
        }
    }

    fun getRegularClass(typeRef: FirTypeRef): FirRegularClass? {
        return typeRef.toRegularClass(firSession)
    }
}

private sealed class ElementToShorten
private class ShortenType(val element: KtUserType, val nameToImport: FqName? = null) : ElementToShorten()
private class ShortenQualifier(val element: KtDotQualifiedExpression, val nameToImport: FqName? = null) : ElementToShorten()

private class ElementsToShortenCollector(
    private val shorteningContext: FirShorteningContext,
    private val towerContextProvider: FirTowerContextProvider
) :
    FirVisitorVoid() {
    val namesToImport: MutableList<FqName> = mutableListOf()
    val typesToShorten: MutableList<KtUserType> = mutableListOf()
    val qualifiersToShorten: MutableList<KtDotQualifiedExpression> = mutableListOf()

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        processTypeRef(resolvedTypeRef)

        resolvedTypeRef.acceptChildren(this)
        resolvedTypeRef.delegatedTypeRef?.accept(this)
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
        super.visitResolvedQualifier(resolvedQualifier)

        processTypeQualifier(resolvedQualifier)
    }

    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
        super.visitResolvedNamedReference(resolvedNamedReference)

        processPropertyReference(resolvedNamedReference)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall) {
        super.visitFunctionCall(functionCall)

        processFunctionCall(functionCall)
    }

    private fun processTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        val wholeTypeReference = resolvedTypeRef.psi as? KtTypeReference ?: return

        val wholeClassifierId = resolvedTypeRef.type.lowerBoundIfFlexible().classId ?: return
        val wholeTypeElement = wholeTypeReference.typeElement?.unwrapNullability() as? KtUserType ?: return

        if (wholeTypeElement.qualifier == null) return

        findTypeToShorten(wholeClassifierId, wholeTypeElement)?.let(::addElementToShorten)
    }

    private fun findTypeToShorten(wholeClassifierId: ClassId, wholeTypeElement: KtUserType): ShortenType? {
        val allClassIds = wholeClassifierId.outerClassesWithSelf
        val allTypeElements = wholeTypeElement.qualifiersWithSelf

        val positionScopes = shorteningContext.findScopesAtPosition(wholeTypeElement, namesToImport, towerContextProvider) ?: return null

        for ((classId, typeElement) in allClassIds.zip(allTypeElements)) {
            // if qualifier is null, then this type have no package and thus cannot be shortened
            if (typeElement.qualifier == null) return null

            val firstFoundClass = shorteningContext.findFirstClassifierInScopesByName(positionScopes, classId.shortClassName)?.classId

            if (firstFoundClass == classId) {
                return ShortenType(typeElement)
            }
        }

        // none class matched
        val (mostTopLevelClassId, mostTopLevelTypeElement) = allClassIds.zip(allTypeElements).last()
        val availableClassifier = shorteningContext.findFirstClassifierInScopesByName(positionScopes, mostTopLevelClassId.shortClassName)

        check(availableClassifier?.classId != mostTopLevelClassId) {
            "If this condition were true, we would have exited from the loop on the last iteration. ClassId = $mostTopLevelClassId"
        }

        return if (availableClassifier == null || availableClassifier.isFromStarOrPackageImport) {
            ShortenType(mostTopLevelTypeElement, mostTopLevelClassId.asSingleFqName())
        } else {
            findFakePackageToShorten(mostTopLevelTypeElement)
        }
    }

    private fun findFakePackageToShorten(typeElement: KtUserType): ShortenType? {
        val deepestTypeWithQualifier = typeElement.qualifiersWithSelf.last().parent as? KtUserType
            ?: error("Type element should have at least one qualifier, instead it was ${typeElement.text}")

        return if (deepestTypeWithQualifier.hasFakeRootPrefix()) ShortenType(deepestTypeWithQualifier) else null
    }

    private fun processTypeQualifier(resolvedQualifier: FirResolvedQualifier) {
        val wholeClassQualifier = resolvedQualifier.classId ?: return
        val wholeQualifierElement = when (val qualifierPsi = resolvedQualifier.psi) {
            is KtDotQualifiedExpression -> qualifierPsi
            is KtNameReferenceExpression -> qualifierPsi.getDotQualifiedExpressionForSelector() ?: return
            else -> return
        }

        findTypeQualifierToShorten(wholeClassQualifier, wholeQualifierElement)?.let(::addElementToShorten)
    }

    private fun findTypeQualifierToShorten(
        wholeClassQualifier: ClassId,
        wholeQualifierElement: KtDotQualifiedExpression
    ): ShortenQualifier? {
        val positionScopes =
            shorteningContext.findScopesAtPosition(wholeQualifierElement, namesToImport, towerContextProvider) ?: return null

        val allClassIds = wholeClassQualifier.outerClassesWithSelf
        val allQualifiers = wholeQualifierElement.qualifiersWithSelf

        for ((classId, qualifier) in allClassIds.zip(allQualifiers)) {
            val firstFoundClass = shorteningContext.findFirstClassifierInScopesByName(positionScopes, classId.shortClassName)?.classId

            if (firstFoundClass == classId) {
                return ShortenQualifier(qualifier)
            }
        }

        val (mostTopLevelClassId, mostTopLevelQualifier) = allClassIds.zip(allQualifiers).last()
        val availableClassifier = shorteningContext.findFirstClassifierInScopesByName(positionScopes, mostTopLevelClassId.shortClassName)

        check(availableClassifier?.classId != mostTopLevelClassId) {
            "If this condition were true, we would have exited from the loop on the last iteration. ClassId = $mostTopLevelClassId"
        }

        return if (availableClassifier == null || availableClassifier.isFromStarOrPackageImport) {
            ShortenQualifier(mostTopLevelQualifier, mostTopLevelClassId.asSingleFqName())
        } else {
            findFakePackageToShorten(mostTopLevelQualifier)
        }
    }

    private fun processPropertyReference(resolvedNamedReference: FirResolvedNamedReference) {
        val referenceExpression = resolvedNamedReference.psi as? KtNameReferenceExpression
        val qualifiedProperty = referenceExpression?.getDotQualifiedExpressionForSelector() ?: return

        val propertyId = (resolvedNamedReference.resolvedSymbol as? FirCallableSymbol<*>)?.callableId ?: return

        val scopes = shorteningContext.findScopesAtPosition(qualifiedProperty, namesToImport, towerContextProvider) ?: return
        val singleAvailableProperty = shorteningContext.findPropertiesInScopes(scopes, propertyId.callableName)

        val propertyToShorten = when {
            singleAvailableProperty.isEmpty() -> ShortenQualifier(qualifiedProperty, propertyId.asImportableFqName())
            singleAvailableProperty.all { it.callableId == propertyId } -> ShortenQualifier(qualifiedProperty)
            else -> findFakePackageToShorten(qualifiedProperty)
        }

        propertyToShorten?.let(::addElementToShorten)
    }

    private fun processFunctionCall(functionCall: FirFunctionCall) {
        if (!canBePossibleToDropReceiver(functionCall)) return

        val qualifiedCallExpression = functionCall.psi as? KtDotQualifiedExpression ?: return
        val callExpression = qualifiedCallExpression.selectorExpression as? KtCallExpression ?: return

        val calleeReference = functionCall.calleeReference
        val callableId = findUnambiguousReferencedCallableId(calleeReference) ?: return

        val scopes = shorteningContext.findScopesAtPosition(callExpression, namesToImport, towerContextProvider) ?: return
        val availableCallables = shorteningContext.findFunctionsInScopes(scopes, callableId.callableName)

        val callToShorten = when {
            availableCallables.isEmpty() -> {
                val additionalImport = callableId.asImportableFqName()
                additionalImport?.let { ShortenQualifier(qualifiedCallExpression, it) }
            }
            availableCallables.all { it.callableId == callableId } -> ShortenQualifier(qualifiedCallExpression)
            else -> findFakePackageToShorten(qualifiedCallExpression)
        }

        callToShorten?.let(::addElementToShorten)
    }

    private fun canBePossibleToDropReceiver(functionCall: FirFunctionCall): Boolean {
        // we can remove receiver only if it is a qualifier
        val explicitReceiver = functionCall.explicitReceiver as? FirResolvedQualifier ?: return false

        // if there is no extension receiver necessary, then it can be removed
        if (functionCall.extensionReceiver is FirNoReceiverExpression) return true

        val receiverType = shorteningContext.getRegularClass(explicitReceiver.typeRef) ?: return true
        return receiverType.classKind != ClassKind.OBJECT
    }

    private fun findUnambiguousReferencedCallableId(namedReference: FirNamedReference): CallableId? {
        val unambiguousSymbol = when (namedReference) {
            is FirResolvedNamedReference -> namedReference.resolvedSymbol
            is FirErrorNamedReference -> {
                val candidateSymbol = namedReference.candidateSymbol
                if (candidateSymbol !is FirErrorFunctionSymbol) {
                    candidateSymbol
                } else {
                    getSingleUnambiguousCandidate(namedReference)
                }
            }
            else -> null
        }

        return (unambiguousSymbol as? FirCallableSymbol<*>)?.callableId
    }

    /**
     * If [namedReference] is ambiguous and all candidates point to the callables with same callableId,
     * returns the first candidate; otherwise returns null.
     */
    private fun getSingleUnambiguousCandidate(namedReference: FirErrorNamedReference): FirCallableSymbol<*>? {
        val coneAmbiguityError = namedReference.diagnostic as? ConeAmbiguityError ?: return null

        val candidates = coneAmbiguityError.candidates.map { it.symbol as FirCallableSymbol<*> }
        require(candidates.isNotEmpty()) { "Cannot have zero candidates" }

        val distinctCandidates = candidates.distinctBy { it.callableId }
        return distinctCandidates.singleOrNull()
            ?: error("Expected all candidates to have same callableId, but got: ${distinctCandidates.map { it.callableId }}")
    }

    private fun findFakePackageToShorten(wholeQualifiedExpression: KtDotQualifiedExpression): ShortenQualifier? {
        val deepestQualifier = wholeQualifiedExpression.qualifiersWithSelf.last()
        return if (deepestQualifier.hasFakeRootPrefix()) ShortenQualifier(deepestQualifier) else null
    }

    private fun addElementToShorten(element: ElementToShorten) {
        when (element) {
            is ShortenType -> {
                namesToImport.addIfNotNull(element.nameToImport)
                typesToShorten.add(element.element)
            }
            is ShortenQualifier -> {
                namesToImport.addIfNotNull(element.nameToImport)
                qualifiersToShorten.add(element.element)
            }
        }
    }

    private val ClassId.outerClassesWithSelf: Sequence<ClassId>
        get() = generateSequence(this) { it.outerClassId }

    private val KtUserType.qualifiersWithSelf: Sequence<KtUserType>
        get() = generateSequence(this) { it.qualifier }

    private val KtDotQualifiedExpression.qualifiersWithSelf: Sequence<KtDotQualifiedExpression>
        get() = generateSequence(this) { it.receiverExpression as? KtDotQualifiedExpression }
}

private class ShortenCommandImpl(
    val targetFile: KtFile,
    val importsToAdd: List<FqName>,
    val typesToShorten: List<SmartPsiElementPointer<KtUserType>>,
    val qualifiersToShorten: List<SmartPsiElementPointer<KtDotQualifiedExpression>>,
) : ShortenCommand {

    override fun invokeShortening() {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        for (nameToImport in importsToAdd) {
            addImportToFile(targetFile.project, targetFile, nameToImport)
        }

        PostprocessReformattingAspect.getInstance(targetFile.project).disablePostprocessFormattingInside {
            for (typePointer in typesToShorten) {
                val type = typePointer.element ?: continue
                type.deleteQualifier()
            }

            for (callPointer in qualifiersToShorten) {
                val call = callPointer.element ?: continue
                call.deleteQualifier()
            }
        }
    }
}

private fun KtUserType.hasFakeRootPrefix(): Boolean =
    qualifier?.referencedName == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE

private fun KtDotQualifiedExpression.hasFakeRootPrefix(): Boolean =
    (receiverExpression as? KtNameReferenceExpression)?.getReferencedName() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE

private fun CallableId.asImportableFqName(): FqName? = if (classId == null) packageName.child(callableName) else null

private fun KtElement.getDotQualifiedExpressionForSelector(): KtDotQualifiedExpression? =
    getQualifiedExpressionForSelector() as? KtDotQualifiedExpression

private fun KtDotQualifiedExpression.deleteQualifier(): KtExpression? {
    val selectorExpression = selectorExpression ?: return null
    return this.replace(selectorExpression) as KtExpression
}
