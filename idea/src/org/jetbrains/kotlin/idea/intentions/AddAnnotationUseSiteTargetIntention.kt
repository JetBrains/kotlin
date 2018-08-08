/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiComment
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class AddAnnotationUseSiteTargetIntention : SelfTargetingIntention<KtAnnotationEntry>(
    KtAnnotationEntry::class.java, "Add use-site target"
) {

    override fun isApplicableTo(element: KtAnnotationEntry, caretOffset: Int): Boolean {
        val useSiteTargets = element.applicableUseSiteTargets()
        if (useSiteTargets.isEmpty()) return false
        if (useSiteTargets.size == 1) {
            text = "Add use-site target '${useSiteTargets.first().renderName}'"
        }
        return true
    }

    override fun applyTo(element: KtAnnotationEntry, editor: Editor?) {
        val project = editor?.project ?: return
        val useSiteTargets = element.applicableUseSiteTargets()
        CommandProcessor.getInstance().runUndoTransparentAction {
            if (useSiteTargets.size == 1)
                element.addUseSiteTarget(useSiteTargets.first(), project)
            else
                JBPopupFactory
                    .getInstance()
                    .createListPopup(createListPopupStep(element, useSiteTargets, project))
                    .showInBestPositionFor(editor)
        }
    }

    private fun createListPopupStep(
        annotationEntry: KtAnnotationEntry,
        useSiteTargets: List<AnnotationUseSiteTarget>,
        project: Project
    ): ListPopupStep<*> {
        return object : BaseListPopupStep<AnnotationUseSiteTarget>("Choose use-site target", useSiteTargets) {
            override fun isAutoSelectionEnabled() = false

            override fun onChosen(selectedValue: AnnotationUseSiteTarget, finalChoice: Boolean): PopupStep<*>? {
                if (finalChoice) {
                    annotationEntry.addUseSiteTarget(selectedValue, project)
                }
                return PopupStep.FINAL_CHOICE
            }

            override fun getIconFor(value: AnnotationUseSiteTarget) = PlatformIcons.ANNOTATION_TYPE_ICON

            override fun getTextFor(value: AnnotationUseSiteTarget) = value.renderName
        }
    }
}

private fun KtAnnotationEntry.applicableUseSiteTargets(): List<AnnotationUseSiteTarget> {
    if (useSiteTarget != null) return emptyList()
    val annotationShortName = this.shortName ?: return emptyList()
    val modifierList = getStrictParentOfType<KtModifierList>() ?: return emptyList()
    val annotated = modifierList.owner as? KtElement ?: return emptyList()

    val applicableTargets = when (annotated) {
        is KtParameter ->
            if (annotated.getStrictParentOfType<KtPrimaryConstructor>() != null)
                when (annotated.valOrVarKeyword?.node?.elementType) {
                    KtTokens.VAR_KEYWORD ->
                        listOf(CONSTRUCTOR_PARAMETER, FIELD, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, SETTER_PARAMETER)
                    KtTokens.VAL_KEYWORD ->
                        listOf(CONSTRUCTOR_PARAMETER, FIELD, PROPERTY, PROPERTY_GETTER)
                    else ->
                        emptyList()
                }
            else
                emptyList()
        is KtProperty ->
            when {
                annotated.delegate != null ->
                    listOf(PROPERTY, PROPERTY_GETTER, PROPERTY_DELEGATE_FIELD)
                !annotated.isLocal -> {
                    val backingField = LightClassUtil.getLightClassPropertyMethods(annotated).backingField
                    if (annotated.isVar) {
                        if (backingField != null)
                            listOf(FIELD, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, SETTER_PARAMETER)
                        else
                            listOf(PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, SETTER_PARAMETER)
                    } else {
                        if (backingField != null)
                            listOf(FIELD, PROPERTY, PROPERTY_GETTER)
                        else
                            listOf(PROPERTY, PROPERTY_GETTER)
                    }
                }
                else ->
                    emptyList()
            }
        is KtTypeReference -> listOf(RECEIVER)
        else -> emptyList()
    }

    val existingTargets = modifierList.annotationEntries.mapNotNull {
        if (annotationShortName == it.shortName) it.useSiteTarget?.getAnnotationUseSiteTarget() else null
    }

    val targets = applicableTargets.filter { it !in existingTargets }

    return if (ApplicationManager.getApplication().isUnitTestMode) {
        val chosenTarget = containingKtFile.findDescendantOfType<PsiComment>()
            ?.takeIf { it.text.startsWith("// CHOOSE_USE_SITE_TARGET:") }
            ?.text
            ?.split(":")
            ?.getOrNull(1)
            ?.trim()
        if (chosenTarget.isNullOrBlank())
            targets.take(1)
        else
            targets.asSequence().filter { it.renderName == chosenTarget }.take(1).toList()
    } else {
        targets
    }
}

private fun KtAnnotationEntry.addUseSiteTarget(useSiteTarget: AnnotationUseSiteTarget, project: Project) {
    project.executeWriteCommand("Add use-site target") {
        replace(KtPsiFactory(this).createAnnotationEntry("@${useSiteTarget.renderName}:$shortName${valueArgumentList?.text ?: ""}"))
    }
}
