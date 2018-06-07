/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.goto

import com.intellij.ide.util.PlatformModuleRendererFactory
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.ide.util.gotoByName.GotoFileCellRenderer
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.FilePathSplittingPolicy
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import java.awt.BorderLayout
import java.awt.Container
import java.io.File
import java.util.*
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

// Mostly copied from com.intellij.ide.actions.SearchEverywherePsiRenderer
// TODO: Drop copied code when SearchEverywherePsiRenderer becomes public
internal class KotlinSearchEverywherePsiRenderer(private val myList: JList<*>) : PsiElementListCellRenderer<PsiElement>() {
    companion object {
        private val RENDERER = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.withOptions {
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
            modifiers = emptySet()
            startFromName = false
        }
    }

    init {
        setFocusBorderEnabled(false)
        layout = object : BorderLayout() {
            override fun layoutContainer(target: Container) {
                super.layoutContainer(target)
                val right = getLayoutComponent(BorderLayout.EAST)
                val left = getLayoutComponent(BorderLayout.WEST)

                //IDEA-140824
                if (right != null && left != null && left.bounds.x + left.bounds.width > right.bounds.x) {
                    val bounds = right.bounds
                    val newX = left.bounds.x + left.bounds.width
                    right.setBounds(newX, bounds.y, bounds.width - (newX - bounds.x), bounds.height)
                }
            }
        }
    }

    override fun getElementText(element: PsiElement?): String {
        if (element !is KtNamedFunction) return ""
        val descriptor = element.resolveToDescriptorIfAny() ?: return ""
        return buildString {
            descriptor.extensionReceiverParameter?.let { append(RENDERER.renderType(it.type)).append('.') }
            append(element.name)
            descriptor.valueParameters.joinTo(this, prefix = "(", postfix = ")") { RENDERER.renderType(it.type) }
        }
    }

    override fun getContainerText(element: PsiElement, name: String): String? {
        if (element is PsiFileSystemItem) {
            val file = element.virtualFile
            val parent = file?.parent
            if (parent == null) {
                if (file != null) { // use fallback from Switcher
                    val presentableUrl = file.presentableUrl
                    return FileUtil.getLocationRelativeToUserHome(presentableUrl)
                }
                return null
            }
            val relativePath = GotoFileCellRenderer.getRelativePath(parent, element.getProject()) ?: return "( " + File.separator + " )"
            var width = myList.width
            if (width == 0) width += 800
            val path = FilePathSplittingPolicy.SPLIT_BY_SEPARATOR.getOptimalTextForComponent(
                    name,
                    File(relativePath),
                    this,
                    width - myRightComponentWidth - 16 - 10
            )
            return "($path)"
        }
        return getSymbolContainerText(name, element)
    }

    private fun getSymbolContainerText(name: String, element: PsiElement): String? {
        var text = SymbolPresentationUtil.getSymbolContainerText(element)

        if (myList.width == 0) return text
        if (text == null) return null

        if (text.startsWith("(") && text.endsWith(")")) {
            text = text.substring(1, text.length - 1)
        }
        val `in` = text.startsWith("in ")
        if (`in`) text = text.substring(3)
        val fm = myList.getFontMetrics(myList.font)
        val maxWidth = myList.width - fm.stringWidth(name) - 16 - myRightComponentWidth - 20
        val left = if (`in`) "(in " else "("
        val right = ")"

        if (fm.stringWidth(left + text + right) < maxWidth) return left + text + right
        val separator = if (text.contains(File.separator)) File.separator else "."
        val parts = LinkedList(StringUtil.split(text, separator))
        var index: Int
        while (parts.size > 1) {
            index = parts.size / 2 - 1
            parts.removeAt(index)
            if (fm.stringWidth(StringUtil.join(parts, separator) + "...") < maxWidth) {
                parts.add(index, "...")
                return left + StringUtil.join(parts, separator) + right
            }
        }
        //todo
        return "$left...$right"
    }


    override fun customizeNonPsiElementLeftRenderer(
        renderer: ColoredListCellRenderer<*>?,
        list: JList<*>?,
        value: Any?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ): Boolean {
        if (value !is NavigationItem) return false

        val item = value as NavigationItem?

        val attributes = getNavigationItemAttributes(item)

        var nameAttributes: SimpleTextAttributes? = if (attributes != null) SimpleTextAttributes.fromTextAttributes(attributes) else null

        val color = list!!.foreground
        if (nameAttributes == null) nameAttributes = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color)

        renderer!!.append(item!!.toString() + " ", nameAttributes)
        val itemPresentation = item.presentation!!
        renderer.icon = itemPresentation.getIcon(true)

        val locationString = itemPresentation.locationString
        if (!StringUtil.isEmpty(locationString)) {
            renderer.append(locationString!!, SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY))
        }
        return true
    }

    override fun getRightCellRenderer(value: Any): DefaultListCellRenderer? {
        val rightRenderer = super.getRightCellRenderer(value)
        return if (rightRenderer is PlatformModuleRendererFactory.PlatformModuleRenderer) {
            // that renderer will display file path, but we're showing it ourselves - no need to show twice
            null
        } else rightRenderer
    }

    override fun getIconFlags() = Iconable.ICON_FLAG_READ_STATUS
}