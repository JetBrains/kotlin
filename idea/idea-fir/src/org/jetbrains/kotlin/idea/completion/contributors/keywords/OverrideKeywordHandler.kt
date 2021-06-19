/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors.keywords

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.RowIcon
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.keywords.CompletionKeywordHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.*
import org.jetbrains.kotlin.idea.core.overrideImplement.KtClassMember
import org.jetbrains.kotlin.idea.core.overrideImplement.KtGenerateMembersHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.KtOverrideMembersHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.generateMember
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.idea.KtIconProvider.getIcon
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.frontend.api.tokens.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.idea.util.application.runWriteAction

internal class OverrideKeywordHandler(
    private val basicContext: FirBasicCompletionContext
) : CompletionKeywordHandler<KtAnalysisSession>(KtTokens.OVERRIDE_KEYWORD) {

    @OptIn(ExperimentalStdlibApi::class)
    override fun KtAnalysisSession.createLookups(
        parameters: CompletionParameters,
        expression: KtExpression?,
        lookup: LookupElement,
        project: Project
    ): Collection<LookupElement> {
        val result = mutableListOf(lookup)
        val position = parameters.position
        val isConstructorParameter = position.getNonStrictParentOfType<KtPrimaryConstructor>() != null
        val classOrObject = position.getNonStrictParentOfType<KtClassOrObject>() ?: return result
        val members = collectMembers(classOrObject, isConstructorParameter)

        for (member in members) {
            result += createLookupElementToGenerateSingleOverrideMember(member, classOrObject, isConstructorParameter, project)
        }
        return result
    }

    private fun KtAnalysisSession.collectMembers(classOrObject: KtClassOrObject, isConstructorParameter: Boolean): List<KtClassMember> {
        val allMembers = KtOverrideMembersHandler().collectMembersToGenerate(classOrObject)
        return if (isConstructorParameter) {
            allMembers.mapNotNull { member ->
                if (member.memberInfo.isProperty) {
                    member.copy(bodyType = BodyType.FROM_TEMPLATE, preferConstructorParameter = true)
                } else null
            }
        } else allMembers.toList()
    }

    private fun KtAnalysisSession.createLookupElementToGenerateSingleOverrideMember(
        member: KtClassMember,
        classOrObject: KtClassOrObject,
        isConstructorParameter: Boolean,
        project: Project
    ): OverridesCompletionLookupElementDecorator {
        val memberSymbol = member.symbol
        check(memberSymbol is KtNamedSymbol)

        val text = getSymbolTextForLookupElement(memberSymbol)
        val baseIcon = getIcon(memberSymbol)
        val isImplement = (memberSymbol as? KtSymbolWithModality)?.modality == Modality.ABSTRACT
        val additionalIcon = if (isImplement) AllIcons.Gutter.ImplementingMethod else AllIcons.Gutter.OverridingMethod
        val icon = RowIcon(baseIcon, additionalIcon)
        val baseClass = classOrObject.getClassOrObjectSymbol()
        val baseClassIcon = getIcon(baseClass)
        val isSuspendFunction = (memberSymbol as? KtFunctionSymbol)?.isSuspend == true
        val baseClassName = baseClass.nameOrAnonymous.asString()

        val memberPointer = memberSymbol.createPointer()

        val baseLookupElement = with(basicContext.lookupElementFactory) { createLookupElement(memberSymbol) }
            ?: error("Lookup element should be available for override completion")
        return OverridesCompletionLookupElementDecorator(
            baseLookupElement,
            declaration = null,
            text,
            isImplement,
            icon,
            baseClassName,
            baseClassIcon,
            isConstructorParameter,
            isSuspendFunction,
            generateMember = {
                generateMemberInNewAnalysisSession(classOrObject, memberPointer, member, project)
            },
            shortenReferences = { element ->
                @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
                val shortenings = hackyAllowRunningOnEdt {
                    analyse(classOrObject) {
                        collectPossibleReferenceShortenings(element.containingKtFile, element.textRange)
                    }
                }
                runWriteAction {
                    shortenings.invokeShortening()
                }
            }
        )
    }

    private fun KtAnalysisSession.getSymbolTextForLookupElement(memberSymbol: KtCallableSymbol): String = buildString {
        append(KtTokens.OVERRIDE_KEYWORD.value)
            .append(" ")
            .append(memberSymbol.render(renderingOptionsForLookupElementRendering))
        if (memberSymbol is KtFunctionSymbol) {
            append(" {...}")
        }
    }

    @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
    private fun generateMemberInNewAnalysisSession(
        classOrObject: KtClassOrObject,
        memberPointer: KtSymbolPointer<KtCallableSymbol>,
        member: KtClassMember,
        project: Project
    ) = hackyAllowRunningOnEdt {
        analyse(classOrObject) {
            val memberInCorrectAnalysisSession = createCopyInCurrentAnalysisSession(memberPointer, member)
            generateMember(
                project,
                memberInCorrectAnalysisSession,
                classOrObject,
                copyDoc = false,
                mode = MemberGenerateMode.OVERRIDE
            )
        }
    }

    //todo temporary hack until KtSymbolPointer is properly implemented
    private fun KtAnalysisSession.createCopyInCurrentAnalysisSession(
        memberPointer: KtSymbolPointer<KtCallableSymbol>,
        member: KtClassMember
    ) = KtClassMember(
        KtClassMemberInfo(
            memberPointer.restoreSymbol()
                ?: error("Cannot restore symbol from $memberPointer"),
            member.memberInfo.memberText,
            member.memberInfo.memberIcon,
            member.memberInfo.containingSymbolText,
            member.memberInfo.containingSymbolIcon,
        ),
        member.bodyType,
        member.preferConstructorParameter,
    )

    companion object {
        private val renderingOptionsForLookupElementRendering =
            KtGenerateMembersHandler.renderOption.copy(
                renderUnitReturnType = false,
                renderDeclarationHeader = true
            )
    }
}