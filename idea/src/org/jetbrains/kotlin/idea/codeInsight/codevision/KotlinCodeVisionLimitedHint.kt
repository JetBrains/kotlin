/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.codevision

import com.intellij.codeInsight.hints.settings.InlayHintsConfigurable
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.highlighter.markers.OVERRIDDEN_FUNCTION
import org.jetbrains.kotlin.idea.highlighter.markers.OVERRIDDEN_PROPERTY
import org.jetbrains.kotlin.idea.highlighter.markers.SUBCLASSED_CLASS
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import java.awt.event.MouseEvent

abstract class KotlinCodeVisionHint(hintKey: String) {
    open val regularText: String = KotlinBundle.message(hintKey)

    abstract fun onClick(editor: Editor, element: PsiElement, event: MouseEvent?)
}

abstract class KotlinCodeVisionLimitedHint(num: Int, limitReached: Boolean, regularHintKey: String, tooManyHintKey: String) :
    KotlinCodeVisionHint(regularHintKey) {

    override val regularText: String =
        if (limitReached) KotlinBundle.message(tooManyHintKey, num) else KotlinBundle.message(regularHintKey, num)
}

private const val IMPLEMENTATIONS_KEY = "hints.codevision.implementations.format"
private const val IMPLEMENTATIONS_TO_MANY_KEY = "hints.codevision.implementations.too_many.format"

private const val INHERITORS_KEY = "hints.codevision.inheritors.format"
private const val INHERITORS_TO_MANY_KEY = "hints.codevision.inheritors.to_many.format"

private const val OVERRIDES_KEY = "hints.codevision.overrides.format"
private const val OVERRIDES_TOO_MANY_KEY = "hints.codevision.overrides.to_many.format"

private const val USAGES_KEY = "hints.codevision.usages.format"
private const val USAGES_TOO_MANY_KEY = "hints.codevision.usages.too_many.format"

private const val SETTINGS_FORMAT = "hints.codevision.settings"

// FUS = Feature Usage Statistics
const val FUS_GROUP_ID = "kotlin.code.vision"
const val USAGES_CLICKED_EVENT_ID = "usages.clicked"
const val INHERITORS_CLICKED_EVENT_ID = "inheritors.clicked"
const val SETTING_CLICKED_EVENT_ID = "setting.clicked"

// FUD = Feature Usage Data
const val FUD_KEY = "location"
const val FUD_FUNCTION = "function"
const val FUD_PROPERTY = "property"
const val FUD_CLASS = "class"
const val FUD_INTERFACE = "interface"

class Usages(usagesNum: Int, limitReached: Boolean) :
    KotlinCodeVisionLimitedHint(usagesNum, limitReached, USAGES_KEY, USAGES_TOO_MANY_KEY) {

    override fun onClick(editor: Editor, element: PsiElement, event: MouseEvent?) {
        logUsageStatistics(editor.project, FUS_GROUP_ID, USAGES_CLICKED_EVENT_ID)
        GotoDeclarationAction.startFindUsages(editor, editor.project!!, element)
    }
}

class FunctionOverrides(overridesNum: Int, limitReached: Boolean) :
    KotlinCodeVisionLimitedHint(overridesNum, limitReached, OVERRIDES_KEY, OVERRIDES_TOO_MANY_KEY) {

    override fun onClick(editor: Editor, element: PsiElement, event: MouseEvent?) {
        val data = FeatureUsageData().addData(FUD_KEY, FUD_FUNCTION)
        logUsageStatistics(editor.project, FUS_GROUP_ID, INHERITORS_CLICKED_EVENT_ID, data)
        val navigationHandler = OVERRIDDEN_FUNCTION.navigationHandler
        navigationHandler.navigate(event, (element as KtFunction).nameIdentifier)
    }
}

class FunctionImplementations(implNum: Int, limitReached: Boolean) :
    KotlinCodeVisionLimitedHint(implNum, limitReached, IMPLEMENTATIONS_KEY, IMPLEMENTATIONS_TO_MANY_KEY) {

    override fun onClick(editor: Editor, element: PsiElement, event: MouseEvent?) {
        val data = FeatureUsageData().addData(FUD_KEY, FUD_FUNCTION)
        logUsageStatistics(editor.project, FUS_GROUP_ID, INHERITORS_CLICKED_EVENT_ID, data)
        val navigationHandler = OVERRIDDEN_FUNCTION.navigationHandler
        navigationHandler.navigate(event, (element as KtFunction).nameIdentifier)
    }
}

class PropertyOverrides(overridesNum: Int, limitReached: Boolean) :
    KotlinCodeVisionLimitedHint(overridesNum, limitReached, OVERRIDES_KEY, OVERRIDES_TOO_MANY_KEY) {

    override fun onClick(editor: Editor, element: PsiElement, event: MouseEvent?) {
        val data = FeatureUsageData().addData(FUD_KEY, FUD_PROPERTY)
        logUsageStatistics(editor.project, FUS_GROUP_ID, INHERITORS_CLICKED_EVENT_ID, data)
        val navigationHandler = OVERRIDDEN_PROPERTY.navigationHandler
        navigationHandler.navigate(event, (element as KtProperty).nameIdentifier)
    }
}

class ClassInheritors(inheritorsNum: Int, limitReached: Boolean) :
    KotlinCodeVisionLimitedHint(inheritorsNum, limitReached, INHERITORS_KEY, INHERITORS_TO_MANY_KEY) {

    override fun onClick(editor: Editor, element: PsiElement, event: MouseEvent?) {
        val data = FeatureUsageData().addData(FUD_KEY, FUD_CLASS)
        logUsageStatistics(editor.project, FUS_GROUP_ID, INHERITORS_CLICKED_EVENT_ID, data)
        val navigationHandler = SUBCLASSED_CLASS.navigationHandler
        navigationHandler.navigate(event, (element as KtClass).nameIdentifier)
    }
}

class InterfaceImplementations(implNum: Int, limitReached: Boolean) :
    KotlinCodeVisionLimitedHint(implNum, limitReached, IMPLEMENTATIONS_KEY, IMPLEMENTATIONS_TO_MANY_KEY) {

    override fun onClick(editor: Editor, element: PsiElement, event: MouseEvent?) {
        val data = FeatureUsageData().addData(FUD_KEY, FUD_INTERFACE)
        logUsageStatistics(editor.project, FUS_GROUP_ID, INHERITORS_CLICKED_EVENT_ID, data)
        val navigationHandler = SUBCLASSED_CLASS.navigationHandler
        navigationHandler.navigate(event, (element as KtClass).nameIdentifier)
    }
}

class SettingsHint : KotlinCodeVisionHint(SETTINGS_FORMAT) {
    override fun onClick(editor: Editor, element: PsiElement, event: MouseEvent?) {
        val project = element.project
        logUsageStatistics(project, FUS_GROUP_ID, SETTING_CLICKED_EVENT_ID)
        InlayHintsConfigurable.showSettingsDialogForLanguage(project, element.language)
    }
}

fun logUsageStatistics(project: Project?, groupId: String, eventId: String) =
    FUCounterUsageLogger.getInstance().logEvent(project, groupId, eventId)

fun logUsageStatistics(project: Project?, groupId: String, eventId: String, data: FeatureUsageData) =
    FUCounterUsageLogger.getInstance().logEvent(project, groupId, eventId, data)