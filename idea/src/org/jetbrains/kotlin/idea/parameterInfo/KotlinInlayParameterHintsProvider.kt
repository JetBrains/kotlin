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

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.hints.Option
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.RenderingFormat
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

/**
 * @author Yuli Fiterman
 */

//hack to separate type presentation from param info presentation
private const val TYPE_INFO_PREFIX = "@TYPE@"
private val typeRenderer = DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.withOptions {
    textFormat = RenderingFormat.PLAIN
}


private enum class HintType(desc: String, enabled: Boolean) {

    PROPERTY_HINT("Show property type hints", true) {
        override fun provideHints(elem: PsiElement): List<InlayInfo> {
            (elem as? KtProperty)?.let {
                property ->
                property.nameIdentifier?.let {
                    ident ->
                    return provideTypeHint(property, ident.endOffset)
                }
            }
            return emptyList()
        }

        override fun isApplicable(elem: PsiElement): Boolean = elem is KtProperty && elem.getReturnTypeReference() == null
    },
    FUNCTION_HINT("Show function type hints", true) {
        override fun provideHints(elem: PsiElement): List<InlayInfo> {
            (elem as? KtNamedFunction)?.let {
                namedFunc ->
                namedFunc.valueParameterList?.let {
                    paramList ->
                    return provideTypeHint(namedFunc, paramList.startOffset)
                }
            }
            return emptyList()
        }

        override fun isApplicable(elem: PsiElement): Boolean = elem is KtNamedFunction && !(elem.hasBlockBody() || elem.hasDeclaredReturnType())
    },
    PARAMETER_TYPE_HINT("Show parameter type hints ", false) {
        override fun provideHints(elem: PsiElement): List<InlayInfo> {
            (elem as? KtParameter)?.let {
                param ->
                param.nameIdentifier?.let {
                    ident ->
                    return provideTypeHint(param, ident.endOffset)
                }
            }
            return emptyList()
        }

        override fun isApplicable(elem: PsiElement): Boolean = elem is KtParameter && elem.typeReference == null
    },
    PARAMETER_HINT("Show parameter hints", true) {
        override fun provideHints(elem: PsiElement): List<InlayInfo> {
            (elem as? KtCallExpression)?.let {
                return provideParameterInfo(it)
            }
            return emptyList()
        }

        override fun isApplicable(elem: PsiElement): Boolean = elem is KtCallExpression
    };

    companion object {

        fun resolve(elem: PsiElement): HintType? = HintType.values().find { it.isApplicable(elem) }
        fun resolveToEnabled(elem: PsiElement?): HintType? {

            val resolved = elem?.let { resolve(it) } ?: return null
            return if (resolved.enabled) {
                resolved
            }
            else {
                null
            }
        }

        private fun provideParameterInfo(element: KtCallExpression): List<InlayInfo> {
            val ctx = element.analyze(BodyResolveMode.PARTIAL)
            element.getResolvedCall(ctx)?.let { resolvedCall ->
                return resolvedCall.valueArguments.mapNotNull { (valueParam: ValueParameterDescriptor, resolvedArg) ->
                    resolvedArg.arguments.firstOrNull()?.let { arg ->
                        arg.getArgumentExpression()?.let { argExp ->
                            if (!arg.isNamed() && !valueParam.name.isSpecial) {
                                return@mapNotNull InlayInfo(valueParam.name.identifier, argExp.startOffset)
                            }
                        }
                    }
                    null
                }
            }

            return emptyList()
        }

        private fun provideTypeHint(element: KtCallableDeclaration, offset: Int): List<InlayInfo> {
            val type = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(element)
            return if (!type.isError) {
                val settings = CodeStyleSettingsManager.getInstance(element.project).currentSettings
                        .getCustomSettings(KotlinCodeStyleSettings::class.java)

                val declString = buildString {
                    append(TYPE_INFO_PREFIX)
                    if (settings.SPACE_BEFORE_TYPE_COLON)
                        append(" ")
                    append(":")
                    if (settings.SPACE_AFTER_TYPE_COLON)
                        append(" ")
                    append(typeRenderer.renderType(type))
                }
                listOf(InlayInfo(declString, offset))
            }
            else {
                emptyList()
            }
        }
    }

    abstract fun isApplicable(elem: PsiElement): Boolean
    abstract fun provideHints(elem: PsiElement): List<InlayInfo>
    val option = Option("SHOW_${this.name}", desc, enabled)
    val enabled
        get() = option.get()
}

class KotlinInlayParameterHintsProvider : InlayParameterHintsProvider {

    override fun getSupportedOptions(): List<Option> = HintType.values().map { it.option }

    override fun getDefaultBlackList(): Set<String> = emptySet()

    override fun getHintInfo(element: PsiElement): HintInfo? {
        val hintType = HintType.resolve(element) ?: return null
        return when (hintType) {
            HintType.PARAMETER_HINT -> (element as? KtCallExpression)?.let { getMethodInfo(it) }
            else -> HintInfo.OptionInfo(hintType.option)
        }
    }

    override fun getParameterHints(element: PsiElement?): List<InlayInfo> = HintType.resolveToEnabled(element)?.provideHints(element!!) ?: emptyList()

    override fun getBlackListDependencyLanguage(): Language = JavaLanguage.INSTANCE

    override fun getInlayPresentation(inlayText: String): String = if (inlayText.startsWith(TYPE_INFO_PREFIX)) {
        inlayText.substring(TYPE_INFO_PREFIX.length)
    }
    else {
        super.getInlayPresentation(inlayText)
    }

    private fun getMethodInfo(elem: KtCallExpression): HintInfo.MethodInfo? {
        val ctx = elem.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = elem.getResolvedCall(ctx)?.candidateDescriptor
        if (resolvedCall is FunctionDescriptor) {
            val fqName = resolvedCall.fqNameOrNull()?.asString() ?: return null
            val paramNames = resolvedCall.valueParameters.map { it.name }.filter { it.isSpecial }.map(Name::asString)
            return HintInfo.MethodInfo(fqName, paramNames)
        }
        return null
    }
}

