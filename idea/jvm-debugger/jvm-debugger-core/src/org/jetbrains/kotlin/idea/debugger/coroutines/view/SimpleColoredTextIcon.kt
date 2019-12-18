/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.view

import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.settings.ThreadsViewSettings
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CoroutineInfoData
import javax.swing.Icon

class SimpleColoredTextIcon(val icon: Icon?, val hasChildrens: Boolean) {
    val texts = mutableListOf<String>()
    val textKeyAttributes = mutableListOf<TextAttributesKey>()

    constructor(icon: Icon?, hasChildrens: Boolean, text: String) : this(icon, hasChildrens) {
        append(text)
    }

    internal fun append(value: String) {
        texts.add(value)
        textKeyAttributes.add(CoroutineDebuggerColors.REGULAR_ATTRIBUTES)
    }

    internal fun appendValue(value: String) {
        texts.add(value)
        textKeyAttributes.add(CoroutineDebuggerColors.VALUE_ATTRIBUTES)
    }

    fun appendToComponent(component: ColoredTextContainer) {
        val size: Int = texts.size
        for (i in 0 until size) {
            val text: String = texts.get(i)
            val attribute: TextAttributesKey = textKeyAttributes.get(i)

            component.append(text, when(attribute) {
                CoroutineDebuggerColors.REGULAR_ATTRIBUTES -> SimpleTextAttributes.REGULAR_ATTRIBUTES
                CoroutineDebuggerColors.VALUE_ATTRIBUTES -> XDebuggerUIConstants.VALUE_NAME_ATTRIBUTES
                else -> SimpleTextAttributes.REGULAR_ATTRIBUTES
            })
        }
    }

    fun forEachTextBlock(f: (Pair<String, TextAttributesKey>) -> Unit) {
        for (pair in texts zip textKeyAttributes)
            f(pair)
    }

    fun simpleString(): String {
        val component = SimpleColoredComponent()
        appendToComponent(component)
        return component.getCharSequence(false).toString()
    }

    fun valuePresentation(): XValuePresentation {
        return object : XValuePresentation() {
            override fun isShowName() = false

            override fun getSeparator() = ""

            override fun renderValue(renderer: XValueTextRenderer) {
                forEachTextBlock {
                    renderer.renderValue(it.first, it.second)
                }
            }

        }
    }
}

interface CoroutineDebuggerColors {
    companion object {
        val REGULAR_ATTRIBUTES =
            TextAttributesKey.createTextAttributesKey("REGULAR_ATTRIBUTES", HighlighterColors.NO_HIGHLIGHTING)
        val VALUE_ATTRIBUTES =
            TextAttributesKey.createTextAttributesKey("VALUE_ATTRIBUTES", CodeInsightColors.WARNINGS_ATTRIBUTES)
    }
}

class SimpleColoredTextIconPresentationRenderer {
    private val settings: ThreadsViewSettings = ThreadsViewSettings.getInstance()

    fun render(infoData: CoroutineInfoData): SimpleColoredTextIcon {
        val thread = infoData.activeThread
        val name = thread?.name()?.substringBefore(" @${infoData.name}") ?: ""
        val threadState = if (thread != null) DebuggerUtilsEx.getThreadStatusText(thread.status()) else ""

        val icon = when (infoData.state) {
            CoroutineInfoData.State.SUSPENDED -> AllIcons.Debugger.ThreadSuspended
            CoroutineInfoData.State.RUNNING -> AllIcons.Debugger.ThreadRunning
            CoroutineInfoData.State.CREATED -> AllIcons.Debugger.ThreadStates.Idle
        }

        val label = SimpleColoredTextIcon(icon, true)
        label.append("\"")
        label.appendValue(infoData.name)
        label.append("\": ${infoData.state}")
        if(name.isNotEmpty()) {
            label.append(" on thread \"")
            label.appendValue(name)
            label.append("\": $threadState")
        }
        return label
    }

    /**
     * Taken from #StackFrameDescriptorImpl.calcRepresentation
     */
    fun render(location: Location): SimpleColoredTextIcon {
        val label = SimpleColoredTextIcon(null, false)
        DebuggerUIUtil.getColorScheme(null)
        if (location.method() != null) {
            val myName = location.method().name()
            val methodDisplay = if (settings.SHOW_ARGUMENTS_TYPES)
                DebuggerUtilsEx.methodNameWithArguments(location.method())
            else
                myName
            label.appendValue(methodDisplay)
        }
        if (settings.SHOW_LINE_NUMBER) {
            label.append(":")
            label.append("" + DebuggerUtilsEx.getLineNumber(location, false))
        }
        if (settings.SHOW_CLASS_NAME) {
            val name: String?
            name = try {
                val refType: ReferenceType = location.declaringType()
                refType.name()
            } catch (e: InternalError) {
                e.toString()
            }
            if (name != null) {
                label.append(", ")
                val dotIndex = name.lastIndexOf('.')
                if (dotIndex < 0) {
                    label.append(name)
                } else {
                    label.append(name.substring(dotIndex + 1))
                    if (settings.SHOW_PACKAGE_NAME) {
                        label.append(" (${name.substring( 0, dotIndex)})")
                    }
                }
            }
        }
        if (settings.SHOW_SOURCE_NAME) {
            label.append(", ")
            val sourceName = DebuggerUtilsEx.getSourceName(location) { e: Throwable? -> "Unknown Source" }
            label.append(sourceName)
        }
        return label
    }

    fun renderCreationNode(infoData: CoroutineInfoData) =
        SimpleColoredTextIcon(AllIcons.Debugger.ThreadSuspended, true, "Creation stack frame of ${infoData.name}")

    fun renderErrorNode(error: String) =
        SimpleColoredTextIcon(AllIcons.Actions.Lightning,false, error)

    fun renderRoorNode(text: String) =
        SimpleColoredTextIcon(null, true, text)

    fun renderGroup(groupName: String) =
        SimpleColoredTextIcon(AllIcons.Debugger.ThreadGroup,true, groupName)
}