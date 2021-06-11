/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.icons.AllIcons
import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import org.jetbrains.kotlin.idea.KotlinIconsIndependent
import java.awt.event.MouseEvent
import javax.swing.Icon

internal class FirStatusBarWidgetFactory: StatusBarWidgetFactory {
    override fun getId(): String = ID

    override fun getDisplayName(): String = "FIR IDE"

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget = Widget()

    override fun disposeWidget(widget: StatusBarWidget) = Disposer.dispose(widget)

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    companion object {
        const val ID = "kotlin.fir.ide"
    }
}

private class Widget : StatusBarWidget, StatusBarWidget.IconPresentation {
    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this
    override fun ID(): String = FirStatusBarWidgetFactory.ID
    override fun getTooltipText(): String = "FIR IDE"
    override fun getIcon(): Icon = KotlinIconsIndependent.FIR

    override fun dispose() {}
    override fun install(statusBar: StatusBar) {}
    override fun getClickConsumer(): Consumer<MouseEvent>? =  null
}