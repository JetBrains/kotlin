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

package org.jetbrains.kotlin.android.folding

import com.android.SdkConstants.*
import com.android.ide.common.resources.configuration.FolderConfiguration
import com.android.ide.common.resources.configuration.LocaleQualifier
import com.android.resources.ResourceType
import com.android.tools.idea.folding.AndroidFoldingSettings
import com.android.tools.idea.res.AppResourceRepository
import com.android.tools.idea.res.LocalResourceRepository
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.SourceTreeToPsiMap
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor
import java.util.regex.Pattern


class ResourceFoldingBuilder : FoldingBuilderEx() {

    companion object {
        // See lint's StringFormatDetector
        private val FORMAT = Pattern.compile("%(\\d+\\$)?([-+#, 0(<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])")
        private val FOLD_MAX_LENGTH = 60
        private val FORCE_PROJECT_RESOURCE_LOADING = true
        private val UNIT_TEST_MODE: Boolean = ApplicationManager.getApplication().isUnitTestMode
        private val RESOURCE_TYPES = listOf(ResourceType.STRING,
                                            ResourceType.DIMEN,
                                            ResourceType.INTEGER,
                                            ResourceType.PLURALS)
    }

    private val isFoldingEnabled = AndroidFoldingSettings.getInstance().isCollapseAndroidStrings

    override fun getPlaceholderText(node: ASTNode): String? {

        tailrec fun UElement.unwrapReferenceAndGetValue(resources: LocalResourceRepository): String? = when (this) {
            is UQualifiedReferenceExpression -> selector.unwrapReferenceAndGetValue(resources)
            is UCallExpression -> (valueArguments.firstOrNull() as? UReferenceExpression)?.getAndroidResourceValue(resources, this)
            else -> (this as? UReferenceExpression)?.getAndroidResourceValue(resources)
        }

        val element = SourceTreeToPsiMap.treeElementToPsi(node) ?: return null
        val appResources = getAppResources(element) ?: return null
        val uastContext = ServiceManager.getService(element.project, UastContext::class.java) ?: return null
        return uastContext.convertElement(element, null, null)?.unwrapReferenceAndGetValue(appResources)
    }

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        if (root !is KtFile || quick && !UNIT_TEST_MODE || !isFoldingEnabled || AndroidFacet.getInstance(root) == null) {
            return emptyArray()
        }

        val file = root.toUElement()
        val result = arrayListOf<FoldingDescriptor>()
        file?.accept(object : AbstractUastVisitor() {
            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression): Boolean {
                node.getFoldingDescriptor()?.let { result.add(it) }
                return super.visitSimpleNameReferenceExpression(node)
            }
        })

        return result.toTypedArray()
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = isFoldingEnabled

    private fun UReferenceExpression.getFoldingDescriptor(): FoldingDescriptor? {
        val resolved = resolve() ?: return null
        val resourceType = resolved.getAndroidResourceType() ?: return null
        if (resourceType !in RESOURCE_TYPES) return null

        fun UElement.createFoldingDescriptor() = psi?.let { psi ->
            val dependencies: Set<PsiElement> = setOf(psi)
            FoldingDescriptor(psi.node, psi.textRange, null, dependencies)
        }

        val element = uastParent as? UQualifiedReferenceExpression ?: this
        val getResourceValueCall = (element.uastParent as? UCallExpression)?.takeIf { it.isFoldableGetResourceValueCall() }
        if (getResourceValueCall != null) {
            val qualifiedCall = getResourceValueCall.uastParent as? UQualifiedReferenceExpression
            if (qualifiedCall?.selector == getResourceValueCall) {
                return qualifiedCall.createFoldingDescriptor()
            }

            return getResourceValueCall.createFoldingDescriptor()
        }

        return element.createFoldingDescriptor()
    }

    private fun UCallExpression.isFoldableGetResourceValueCall(): Boolean {
        return methodName == "getString" ||
               methodName == "getText" ||
               methodName == "getInteger" ||
               methodName?.startsWith("getDimension") ?: false ||
               methodName?.startsWith("getQuantityString") ?: false
    }

    private fun PsiElement.getAndroidResourceType(): ResourceType? {
        val elementType = parent as? PsiClass ?: return null
        val elementPackage = elementType.parent as? PsiClass ?: return null
        if (R_CLASS != elementPackage.name) return null
        if (elementPackage.qualifiedName != "$ANDROID_PKG.$R_CLASS") {
            return ResourceType.getEnum(elementType.name)
        }

        return null
    }

    private fun UReferenceExpression.getAndroidResourceValue(resources: LocalResourceRepository, call: UCallExpression? = null): String? {
        val resourceType = resolve()?.getAndroidResourceType() ?: return null
        val referenceConfig = FolderConfiguration().apply { localeQualifier = LocaleQualifier("xx") }
        val key = resolvedName ?: return null
        val resourceValue = resources.getResourceValue(resourceType, key, referenceConfig) ?: return null
        val text = if (call != null) formatArguments(call, resourceValue) else resourceValue

        if (resourceType == ResourceType.STRING || resourceType == ResourceType.PLURALS) {
            return '"' + StringUtil.shortenTextWithEllipsis(text, FOLD_MAX_LENGTH - 2, 0) + '"'
        }
        else if (text.length <= 1) {
            // Don't just inline empty or one-character replacements: they can't be expanded by a mouse click
            // so are hard to use without knowing about the folding keyboard shortcut to toggle folding.
            // This is similar to how IntelliJ 14 handles call parameters
            return key + ": " + text
        }

        return StringUtil.shortenTextWithEllipsis(text, FOLD_MAX_LENGTH, 0)
    }

    private tailrec fun LocalResourceRepository.getResourceValue(
            type: ResourceType,
            name: String,
            referenceConfig: FolderConfiguration): String? {
        val value = getConfiguredValue(type, name, referenceConfig)?.value ?: return null
        if (!value.startsWith('@')) {
            return value
        }

        val (referencedTypeName, referencedName) = value.substring(1).split('/').takeIf { it.size == 2 } ?: return value
        val referencedType = ResourceType.getEnum(referencedTypeName) ?: return value
        return getResourceValue(referencedType, referencedName, referenceConfig)
    }

    // Converted from com.android.tools.idea.folding.InlinedResource#insertArguments
    private fun formatArguments(callExpression: UCallExpression, formatString: String): String {
        if (!formatString.contains('%')) {
            return formatString
        }

        val args = callExpression.valueArguments
        if (args.isEmpty() || !args.first().isPsiValid) {
            return formatString
        }

        val matcher = FORMAT.matcher(formatString)
        var index = 0
        var prevIndex = 0
        var nextNumber = 1
        var start = 0
        val sb = StringBuilder(2 * formatString.length)
        while (true) {
            if (matcher.find(index)) {
                if ("%" == matcher.group(6)) {
                    index = matcher.end()
                    continue
                }
                val matchStart = matcher.start()
                // Make sure this is not an escaped '%'
                while (prevIndex < matchStart) {
                    val c = formatString[prevIndex]
                    if (c == '\\') {
                        prevIndex++
                    }
                    prevIndex++
                }
                if (prevIndex > matchStart) {
                    // We're in an escape, ignore this result
                    index = prevIndex
                    continue
                }

                index = matcher.end()

                // Shouldn't throw a number format exception since we've already
                // matched the pattern in the regexp
                val number: Int
                var numberString: String? = matcher.group(1)
                if (numberString != null) {
                    // Strip off trailing $
                    numberString = numberString.substring(0, numberString.length - 1)
                    number = Integer.parseInt(numberString)
                    nextNumber = number + 1
                }
                else {
                    number = nextNumber++
                }

                if (number > 0 && number < args.size) {
                    val argExpression = args[number]
                    var value: Any? = argExpression.evaluate()

                    if (value == null) {
                        value = args[number].asSourceString()
                    }

                    for (i in start..matchStart - 1) {
                        sb.append(formatString[i])
                    }

                    sb.append("{")
                    sb.append(value)
                    sb.append('}')
                    start = index
                }
            }
            else {
                var i = start
                val n = formatString.length
                while (i < n) {
                    sb.append(formatString[i])
                    i++
                }
                break
            }
        }

        return sb.toString()
    }

    private fun getAppResources(element: PsiElement): LocalResourceRepository? = ModuleUtilCore.findModuleForPsiElement(element)?.let {
        AppResourceRepository.getAppResources(it, FORCE_PROJECT_RESOURCE_LOADING)
    }
}
