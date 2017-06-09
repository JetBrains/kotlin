/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android

import com.android.SdkConstants
import com.android.resources.ResourceType
import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.icons.AllIcons
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.navigation.GotoRelatedItem
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Function
import org.jetbrains.android.dom.manifest.Manifest
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.resourceManagers.ModuleResourceManagers
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.awt.event.MouseEvent
import javax.swing.Icon


class KotlinAndroidLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
        elements.forEach {
            (it as? KtClass)?.getLineMarkerInfo()?.let { marker -> result.add(marker) }
        }
    }

    private fun KtClass.getLineMarkerInfo(): LineMarkerInfo<PsiElement>? {
        val nameIdentifier = nameIdentifier ?: return null
        val androidFacet = AndroidFacet.getInstance(this) ?: return null
        val manifest = androidFacet.manifest ?: return null
        val manifestItems = collectGoToRelatedManifestItems(manifest)
        if (manifestItems.isEmpty() && !isClassWithLayoutXml()) {
            return null
        }

        return LineMarkerInfo(
                    nameIdentifier,
                    nameIdentifier.textRange,
                    AllIcons.FileTypes.Xml,
                    Pass.LINE_MARKERS,
                    Function { "Related XML file" },
                    GutterIconNavigationHandler { e: MouseEvent, _: PsiElement ->
                        NavigationUtil
                                .getRelatedItemsPopup(
                                        manifestItems + collectGoToRelatedLayoutItems(androidFacet),
                                        "Go to Related Files")
                                .show(RelativePoint(e))
                    },
                    GutterIconRenderer.Alignment.RIGHT)
    }

    private fun KtClass.collectGoToRelatedLayoutItems(androidFacet: AndroidFacet): List<GotoRelatedItem> {
        val resources = mutableSetOf<PsiFile>()
        accept(object: KtVisitorVoid() {
            override fun visitKtElement(element: KtElement) {
                element.acceptChildren(this)
            }

            override fun visitReferenceExpression(expression: KtReferenceExpression) {
                super.visitReferenceExpression(expression)

                val resClassName = ResourceType.LAYOUT.getName()
                val info = (expression as? KtSimpleNameExpression)?.let {
                    getReferredResourceOrManifestField(androidFacet, it, resClassName, true)
                }

                if (info == null || info.isFromManifest) {
                    return
                }

                val files = ModuleResourceManagers
                        .getInstance(androidFacet)
                        .localResourceManager
                        .findResourcesByFieldName(resClassName, info.fieldName)
                        .filterIsInstance<PsiFile>()

                resources.addAll(files)
            }
        })

        return resources.map { GotoRelatedLayoutItem(it) }
    }

    private fun KtClass.collectGoToRelatedManifestItems(manifest: Manifest): List<GotoRelatedItem> =
            findComponentDeclarationInManifest(manifest)?.xmlAttributeValue?.let { listOf(GotoManifestItem(it)) } ?: emptyList()

    private class GotoManifestItem(attributeValue: XmlAttributeValue) : GotoRelatedItem(attributeValue) {
        override fun getCustomName(): String? = "AndroidManifest.xml"
        override fun getCustomContainerName(): String? = ""
        override fun getCustomIcon(): Icon? = XmlFileType.INSTANCE.icon
    }

    private class GotoRelatedLayoutItem(private val file: PsiFile) : GotoRelatedItem(file, "Layout Files") {
        override fun getCustomContainerName(): String? = "(${file.containingDirectory.name})"
    }

    companion object {
        private val CLASSES_WITH_LAYOUT_XML = arrayOf(
                SdkConstants.CLASS_ACTIVITY,
                SdkConstants.CLASS_FRAGMENT,
                SdkConstants.CLASS_V4_FRAGMENT,
                "android.widget.Adapter")

        private fun KtClass.isClassWithLayoutXml(): Boolean {
            val type = (resolveToDescriptor(BodyResolveMode.PARTIAL) as? ClassDescriptor)?.defaultType ?: return false
            return CLASSES_WITH_LAYOUT_XML.any { type.isSubclassOf(it, true) }
        }
    }
}