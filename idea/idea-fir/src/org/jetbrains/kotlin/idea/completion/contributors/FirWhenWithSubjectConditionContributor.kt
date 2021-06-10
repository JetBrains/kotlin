/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.parentOfType
import gnu.trove.THashSet
import gnu.trove.TObjectHashingStrategy
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.completion.InsertionHandlerBase
import org.jetbrains.kotlin.idea.completion.KotlinFirIconProvider.getIconFor
import org.jetbrains.kotlin.idea.completion.checkers.CompletionVisibilityChecker
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirWithSubjectEntryPositionContext
import org.jetbrains.kotlin.idea.completion.createKeywordElement
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.completion.contributors.helpers.FirClassifierProvider.getAvailableClassifiersCurrentScope
import org.jetbrains.kotlin.idea.completion.contributors.helpers.insertSymbol
import org.jetbrains.kotlin.idea.completion.contributors.helpers.addTypeArguments
import org.jetbrains.kotlin.idea.completion.contributors.helpers.createStarTypeArgumentsList
import org.jetbrains.kotlin.idea.completion.lookups.KotlinLookupObject
import org.jetbrains.kotlin.idea.completion.lookups.shortenReferencesForFirCompletion
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithTypeParameters
import org.jetbrains.kotlin.idea.frontend.api.types.*
import org.jetbrains.kotlin.miniStdLib.letIf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.render

internal class FirWhenWithSubjectConditionContributor(
    basicContext: FirBasicCompletionContext,
    priority: Int,
) : FirCompletionContributorBase<FirWithSubjectEntryPositionContext>(basicContext, priority) {
    override fun KtAnalysisSession.complete(positionContext: FirWithSubjectEntryPositionContext) {
        val whenCondition = positionContext.whenCondition
        val whenExpression = whenCondition.parentOfType<KtWhenExpression>() ?: return
        val subject = whenExpression.subjectExpression ?: return
        val allConditionsExceptCurrent = whenExpression.entries.flatMap { entry -> entry.conditions.filter { it != whenCondition } }
        val subjectType = subject.getKtType()
        val classSymbol = getClassSymbol(subjectType)
        val visibilityChecker = CompletionVisibilityChecker.create(basicContext, positionContext)
        val isSingleCondition = whenCondition.isSingleConditionInEntry()
        when {
            classSymbol?.classKind == KtClassKind.ENUM_CLASS -> {
                completeEnumEntries(classSymbol, allConditionsExceptCurrent, visibilityChecker, isSingleCondition)
            }
            classSymbol?.modality == Modality.SEALED -> {
                completeSubClassesOfSealedClass(
                    classSymbol,
                    allConditionsExceptCurrent,
                    whenCondition,
                    visibilityChecker,
                    isSingleCondition
                )
            }
            else -> {
                completeAllTypes(whenCondition, visibilityChecker, isSingleCondition)
            }
        }
        addNullIfWhenExpressionCanReturnNull(subjectType)
        addElseBranchIfSingleConditionInEntry(whenCondition)
    }

    private fun KtAnalysisSession.getClassSymbol(subjectType: KtType): KtNamedClassOrObjectSymbol? {
        val classType = subjectType as? KtNonErrorClassType
        return classType?.classSymbol as? KtNamedClassOrObjectSymbol
    }


    private fun KtAnalysisSession.addNullIfWhenExpressionCanReturnNull(type: KtType?) {
        if (type?.canBeNull == true) {
            val lookupElement = createKeywordElement(keyword = KtTokens.NULL_KEYWORD.value)
            sink.addElement(lookupElement)
        }
    }

    private fun KtAnalysisSession.completeAllTypes(
        whenCondition: KtWhenCondition,
        visibilityChecker: CompletionVisibilityChecker,
        isSingleCondition: Boolean,
    ) {
        getAvailableClassifiersCurrentScope(originalKtFile, whenCondition, scopeNameFilter, indexHelper, visibilityChecker)
            .forEach { classifier ->
                if (classifier !is KtNamedSymbol) return@forEach

                addLookupElement(
                    classifier.name.asString(),
                    classifier,
                    (classifier as? KtNamedClassOrObjectSymbol)?.classIdIfNonLocal?.asSingleFqName(),
                    isPrefixNeeded(classifier),
                    isSingleCondition,
                )
            }
    }

    private fun KtAnalysisSession.isPrefixNeeded(classifier: KtClassifierSymbol): Boolean {
        return when (classifier) {
            is KtAnonymousObjectSymbol -> return false
            is KtNamedClassOrObjectSymbol -> !classifier.classKind.isObject
            is KtTypeAliasSymbol -> (classifier.expandedType as? KtNonErrorClassType)?.classSymbol?.let { isPrefixNeeded(it) } == true
            is KtTypeParameterSymbol -> true
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun KtAnalysisSession.completeSubClassesOfSealedClass(
        classSymbol: KtNamedClassOrObjectSymbol,
        conditions: List<KtWhenCondition>,
        whenCondition: KtWhenCondition,
        visibilityChecker: CompletionVisibilityChecker,
        isSingleCondition: Boolean,
    ) {
        require(classSymbol.modality == Modality.SEALED)
        val handledCasesClassIds = getHandledClassIds(conditions)
        val allInheritors = getAllSealedInheritors(classSymbol)

        allInheritors
            .asSequence()
            .filter { it.classIdIfNonLocal !in handledCasesClassIds }
            .filter { with(visibilityChecker) { isVisible(it as KtClassifierSymbol) } }
            .forEach { inheritor ->
                val classId = inheritor.classIdIfNonLocal ?: return@forEach
                addLookupElement(
                    classId.relativeClassName.asString(),
                    inheritor,
                    classId.asSingleFqName(),
                    isPrefixNeeded(inheritor),
                    isSingleCondition
                )
            }

        if (allInheritors.any { it.modality == Modality.ABSTRACT }) {
            completeAllTypes(whenCondition, visibilityChecker, isSingleCondition)
        }
    }

    private fun KtAnalysisSession.getHandledClassIds(conditions: List<KtWhenCondition>): Set<ClassId> =
        conditions.mapNotNullTo(hashSetOf()) { condition ->
            val reference = when (condition) {
                is KtWhenConditionWithExpression -> condition.expression?.reference()
                is KtWhenConditionIsPattern -> (condition.typeReference?.typeElement as? KtUserType)?.referenceExpression?.reference()
                else -> null
            }
            val resolvesTo = reference?.resolveToSymbol() as? KtNamedClassOrObjectSymbol
            resolvesTo?.classIdIfNonLocal
        }

    private fun KtAnalysisSession.getAllSealedInheritors(classSymbol: KtNamedClassOrObjectSymbol): Collection<KtNamedClassOrObjectSymbol> {
        fun KtAnalysisSession.getAllSealedInheritorsTo(
            classSymbol: KtNamedClassOrObjectSymbol,
            destination: MutableSet<KtNamedClassOrObjectSymbol>
        ) {
            classSymbol.getSealedClassInheritors().forEach { inheritor ->
                destination += inheritor
                if (inheritor.modality == Modality.SEALED) {
                    getAllSealedInheritorsTo(inheritor, destination)
                }
            }
        }

        return THashSet(KtNamedClassOrObjectSymbolTObjectHashingStrategy)
            .apply { getAllSealedInheritorsTo(classSymbol, this) }
    }

    private fun addElseBranchIfSingleConditionInEntry(whenCondition: KtWhenCondition) {
        val whenEntry = whenCondition.parent as? KtWhenEntry ?: return
        if (whenEntry.conditions.size > 1) return
        val lookupElement = createKeywordElement(keyword = KtTokens.ELSE_KEYWORD.value, tail = " -> ")
        sink.addElement(lookupElement)
    }


    private fun KtAnalysisSession.completeEnumEntries(
        classSymbol: KtNamedClassOrObjectSymbol,
        conditions: List<KtWhenCondition>,
        visibilityChecker: CompletionVisibilityChecker,
        isSingleCondition: Boolean,
    ) {
        require(classSymbol.classKind == KtClassKind.ENUM_CLASS)
        val handledCasesNames = conditions.mapNotNullTo(hashSetOf()) { condition ->
            val conditionWithExpression = condition as? KtWhenConditionWithExpression
            val resolvesTo = conditionWithExpression?.expression?.reference()?.resolveToSymbol() as? KtEnumEntrySymbol
            resolvesTo?.name
        }
        val allEnumEntrySymbols = classSymbol.getEnumEntries()
        allEnumEntrySymbols
            .filter { it.name !in handledCasesNames }
            .filter { with(visibilityChecker) { isVisible(it) } }
            .forEach { entry ->
                addLookupElement(
                    "${classSymbol.name.asString()}.${entry.name.asString()}",
                    entry,
                    entry.callableIdIfNonLocal?.asSingleFqName(),
                    isPrefixNeeded = false,
                    isSingleCondition,
                )
            }
    }

    private fun KtWhenCondition.isSingleConditionInEntry(): Boolean {
        val entry = parent as KtWhenEntry
        return entry.conditions.size == 1
    }

    private fun KtAnalysisSession.addLookupElement(
        lookupString: String,
        symbol: KtNamedSymbol,
        fqName: FqName?,
        isPrefixNeeded: Boolean,
        isSingleCondition: Boolean
    ) {
        val typeArgumentsCount = (symbol as? KtSymbolWithTypeParameters)?.typeParameters?.size ?: 0
        val lookupObject = WhenConditionLookupObject(symbol.name, fqName, isPrefixNeeded, isSingleCondition, typeArgumentsCount)

        LookupElementBuilder.create(lookupObject, getIsPrefix(isPrefixNeeded) + lookupString)
            .withIcon(getIconFor(symbol))
            .withPsiElement(symbol.psi)
            .withInsertHandler(WhenConditionInsertionHandler)
            .withTailText(createStarTypeArgumentsList(typeArgumentsCount), /*grayed*/true)
            .letIf(isSingleCondition) { it.appendTailText(" -> ",  /*grayed*/true) }
            .let(sink::addElement)
    }
}

private data class WhenConditionLookupObject(
    override val shortName: Name,
    val fqName: FqName?,
    val needIsPrefix: Boolean,
    val isSingleCondition: Boolean,
    val typeArgumentsCount: Int,
) : KotlinLookupObject


private object WhenConditionInsertionHandler : InsertionHandlerBase<WhenConditionLookupObject>(WhenConditionLookupObject::class) {
    override fun handleInsert(context: InsertionContext, item: LookupElement, ktFile: KtFile, lookupObject: WhenConditionLookupObject) {
        context.insertName(lookupObject, ktFile)
        context.addTypeArguments(lookupObject.typeArgumentsCount)
        context.addArrow(lookupObject)
    }

    private fun InsertionContext.addArrow(
        lookupObject: WhenConditionLookupObject
    ) {
        if (lookupObject.isSingleCondition && completionChar != ',') {
            insertSymbol(" -> ")
            commitDocument()
        }
    }

    private fun InsertionContext.insertName(
        lookupObject: WhenConditionLookupObject,
        ktFile: KtFile
    ) {
        if (lookupObject.fqName != null) {
            val fqName = lookupObject.fqName
            document.replaceString(
                startOffset,
                tailOffset,
                getIsPrefix(lookupObject.needIsPrefix) + fqName.render()
            )
            commitDocument()

            shortenReferencesForFirCompletion(ktFile, TextRange(startOffset, tailOffset))
        }
    }
}

private fun getIsPrefix(prefixNeeded: Boolean): String {
    return if (prefixNeeded) "is " else ""
}

private object KtNamedClassOrObjectSymbolTObjectHashingStrategy : TObjectHashingStrategy<KtNamedClassOrObjectSymbol> {
    override fun equals(p0: KtNamedClassOrObjectSymbol, p1: KtNamedClassOrObjectSymbol): Boolean =
        p0.classIdIfNonLocal == p1.classIdIfNonLocal

    override fun computeHashCode(p0: KtNamedClassOrObjectSymbol): Int =
        p0.classIdIfNonLocal.hashCode()
}
