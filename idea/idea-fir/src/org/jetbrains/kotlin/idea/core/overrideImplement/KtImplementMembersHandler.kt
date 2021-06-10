/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.overrideImplement

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.core.overrideImplement.KtImplementMembersHandler.Companion.getUnimplementedMembers
import org.jetbrains.kotlin.idea.core.util.KotlinIdeaCoreBundle
import org.jetbrains.kotlin.idea.fir.api.fixes.diagnosticFixFactoryFromIntentionActions
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtIconProvider.getIcon
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.idea.frontend.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.frontend.api.tokens.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.util.ImplementationStatus

internal open class KtImplementMembersHandler : KtGenerateMembersHandler() {
    override fun getChooserTitle() = KotlinIdeaCoreBundle.message("implement.members.handler.title")

    override fun getNoMembersFoundHint() = KotlinIdeaCoreBundle.message("implement.members.handler.no.members.hint")

    @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
    override fun collectMembersToGenerate(classOrObject: KtClassOrObject): Collection<KtClassMember> {
        return hackyAllowRunningOnEdt {
            analyse(classOrObject) {
                getUnimplementedMembers(classOrObject).map { createKtClassMember(it, BodyType.FROM_TEMPLATE, false) }
            }
        }
    }

    companion object {
        fun KtAnalysisSession.getUnimplementedMembers(classWithUnimplementedMembers: KtClassOrObject): List<KtClassMemberInfo> {
            return getUnimplementedMemberSymbols(classWithUnimplementedMembers.getClassOrObjectSymbol()).map { unimplementedMemberSymbol ->
                val containingSymbol = unimplementedMemberSymbol.originalContainingClassForOverride
                KtClassMemberInfo(
                    symbol = unimplementedMemberSymbol,
                    memberText = unimplementedMemberSymbol.render(renderOption),
                    memberIcon = getIcon(unimplementedMemberSymbol),
                    containingSymbolText = containingSymbol?.classIdIfNonLocal?.asSingleFqName()?.toString()
                        ?: containingSymbol?.name?.asString(),
                    containingSymbolIcon = containingSymbol?.let { symbol -> getIcon(symbol) }
                )
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        private fun KtAnalysisSession.getUnimplementedMemberSymbols(classWithUnimplementedMembers: KtClassOrObjectSymbol): List<KtCallableSymbol> {
            return buildList {
                classWithUnimplementedMembers.getMemberScope().getCallableSymbols().forEach { symbol ->
                    if (!symbol.isVisibleInClass(classWithUnimplementedMembers)) return@forEach
                    when (symbol.getImplementationStatus(classWithUnimplementedMembers)) {
                        ImplementationStatus.NOT_IMPLEMENTED -> add(symbol)
                        ImplementationStatus.AMBIGUOUSLY_INHERITED,
                        ImplementationStatus.INHERITED_OR_SYNTHESIZED -> {
                            // This case is to show user abstract members that don't need to be implemented because another super class has provide
                            // an implementation. For example, given the following
                            //
                            // interface A { fun foo() }
                            // interface B { fun foo() {} }
                            // class Foo : A, B {}
                            //
                            // `Foo` does not need to implement `foo` since it inherits the implementation from `B`. But in the dialog, we should
                            // allow user to choose `foo` to implement.
                            symbol.getIntersectionOverriddenSymbols()
                                .filter { (it as? KtSymbolWithModality)?.modality == Modality.ABSTRACT }
                                .forEach { add(it) }
                        }
                        else -> {
                        }
                    }
                }
            }
        }
    }
}

internal class KtImplementMembersQuickfix(private val members: Collection<KtClassMemberInfo>) : KtImplementMembersHandler(),
    IntentionAction {
    override fun getText() = familyName
    override fun getFamilyName() = KotlinIdeaCoreBundle.message("implement.members.handler.family")

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile) = isValidFor(editor, file)

    override fun collectMembersToGenerate(classOrObject: KtClassOrObject): Collection<KtClassMember> {
        return members.map { createKtClassMember(it, BodyType.FROM_TEMPLATE, false) }
    }
}

internal class KtImplementAsConstructorParameterQuickfix(private val members: Collection<KtClassMemberInfo>) : KtImplementMembersHandler(),
    IntentionAction {
    override fun getText() = KotlinIdeaCoreBundle.message("action.text.implement.as.constructor.parameters")

    override fun getFamilyName() = KotlinIdeaCoreBundle.message("implement.members.handler.family")

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile) = isValidFor(editor, file)

    override fun isValidForClass(classOrObject: KtClassOrObject): Boolean {
        if (classOrObject !is KtClass || classOrObject is KtEnumEntry || classOrObject.isInterface()) return false
        // TODO: when MPP support is ready, return false if this class is `actual` and any expect classes have primary constructor.
        return members.any { it.isProperty }
    }

    override fun collectMembersToGenerate(classOrObject: KtClassOrObject): Collection<KtClassMember> {
        return members.filter { it.isProperty }.map { createKtClassMember(it, BodyType.FROM_TEMPLATE, true) }
    }
}

object MemberNotImplementedQuickfixFactories {

    val abstractMemberNotImplemented =
        diagnosticFixFactoryFromIntentionActions(KtFirDiagnostic.AbstractMemberNotImplemented::class) { diagnostic ->
            getUnimplementedMemberFixes(diagnostic.psi)
        }

    val abstractClassMemberNotImplemented =
        diagnosticFixFactoryFromIntentionActions(KtFirDiagnostic.AbstractClassMemberNotImplemented::class) { diagnostic ->
            getUnimplementedMemberFixes(diagnostic.psi)
        }

    val manyInterfacesMemberNotImplemented =
        diagnosticFixFactoryFromIntentionActions(KtFirDiagnostic.ManyInterfacesMemberNotImplemented::class) { diagnostic ->
            getUnimplementedMemberFixes(diagnostic.psi)
        }

    val manyImplMemberNotImplemented =
        diagnosticFixFactoryFromIntentionActions(KtFirDiagnostic.ManyImplMemberNotImplemented::class) { diagnostic ->
            getUnimplementedMemberFixes(diagnostic.psi, false)
        }

    @OptIn(ExperimentalStdlibApi::class)
    private fun KtAnalysisSession.getUnimplementedMemberFixes(
        classWithUnimplementedMembers: KtClassOrObject,
        includeImplementAsConstructorParameterQuickfix: Boolean = true
    ): List<IntentionAction> {
        val unimplementedMembers = getUnimplementedMembers(classWithUnimplementedMembers)

        return buildList {
            add(KtImplementMembersQuickfix(unimplementedMembers))
            if (includeImplementAsConstructorParameterQuickfix) {
                add(KtImplementAsConstructorParameterQuickfix(unimplementedMembers))
            }
        }
    }
}
