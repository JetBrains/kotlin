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

/*

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.hints.Option
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

enum class HintType(desc: String, enabled: Boolean) {

    PROPERTY_HINT("Show property type hints", false) {
        override fun provideHints(elem: PsiElement): List<InlayInfo> {
            return providePropertyTypeHint(elem)
        }

        override fun isApplicable(elem: PsiElement): Boolean = elem is KtProperty && elem.getReturnTypeReference() == null && !elem.isLocal
    },

    LOCAL_VARIABLE_HINT("Show local variable type hints", false) {
        override fun provideHints(elem: PsiElement): List<InlayInfo> {
            return providePropertyTypeHint(elem)
        }

        override fun isApplicable(elem: PsiElement): Boolean = (elem is KtProperty && elem.getReturnTypeReference() == null && elem.isLocal) ||
                                                               (elem is KtParameter && elem.isLoopParameter && elem.typeReference == null) ||
                                                               (elem is KtDestructuringDeclarationEntry)
    },

    FUNCTION_HINT("Show function return type hints", false) {
        override fun provideHints(elem: PsiElement): List<InlayInfo> {
            (elem as? KtNamedFunction)?.let { namedFunc ->
                namedFunc.valueParameterList?.let { paramList ->
                    return provideTypeHint(namedFunc, paramList.endOffset)
                }
            }
            return emptyList()
        }

        override fun isApplicable(elem: PsiElement): Boolean = elem is KtNamedFunction && !(elem.hasBlockBody() || elem.hasDeclaredReturnType())
    },
    PARAMETER_TYPE_HINT("Show parameter type hints ", false) {
        override fun provideHints(elem: PsiElement): List<InlayInfo> {
            (elem as? KtParameter)?.let { param ->
                param.nameIdentifier?.let { ident ->
                    return provideTypeHint(param, ident.endOffset)
                }
            }
            return emptyList()
        }

        override fun isApplicable(elem: PsiElement): Boolean = elem is KtParameter && elem.typeReference == null && !elem.isLoopParameter
    },
    PARAMETER_HINT("Show argument name hints", true) {
        override fun provideHints(elem: PsiElement): List<InlayInfo> {
            (elem as? KtCallElement)?.let {
                return provideArgumentNameHints(it)
            }
            return emptyList()
        }

        override fun isApplicable(elem: PsiElement): Boolean = elem is KtCallElement
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
    }

    abstract fun isApplicable(elem: PsiElement): Boolean
    abstract fun provideHints(elem: PsiElement): List<InlayInfo>
    val option = Option("SHOW_${this.name}", desc, enabled)
    val enabled
        get() = option.get()
}

class KotlinInlayParameterHintsProvider : InlayParameterHintsProvider {

    override fun getSupportedOptions(): List<Option> = HintType.values().map { it.option }

    override fun getDefaultBlackList(): Set<String> = setOf("*listOf", "*setOf", "*arrayOf", "*ListOf", "*SetOf", "*ArrayOf")

    override fun getHintInfo(element: PsiElement): HintInfo? {
        val hintType = HintType.resolve(element) ?: return null
        return when (hintType) {
            HintType.PARAMETER_HINT -> (element as? KtCallElement)?.let { getMethodInfo(it) }
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

    private fun getMethodInfo(elem: KtCallElement): HintInfo.MethodInfo? {
        val ctx = elem.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = elem.getResolvedCall(ctx)
        val resolvedCallee = resolvedCall?.candidateDescriptor
        if (resolvedCallee is FunctionDescriptor) {
            val fqName = if (resolvedCallee is ConstructorDescriptor)
                resolvedCallee.containingDeclaration.fqNameSafe.asString()
            else
                (resolvedCallee.fqNameOrNull()?.asString() ?: return null)
            val paramNames = resolvedCall.valueArguments
                    .mapNotNull { (valueParameterDescriptor, resolvedValueArgument) ->
                        if (resolvedValueArgument !is DefaultValueArgument) valueParameterDescriptor.name else null
                    }
                    .filter { !it.isSpecial }
                    .map(Name::asString)
            return HintInfo.MethodInfo(fqName, paramNames)
        }
        return null
    }
}
*/

