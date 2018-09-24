/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.hints.Option
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

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

        override fun isApplicable(elem: PsiElement): Boolean =
            (elem is KtProperty && elem.getReturnTypeReference() == null && elem.isLocal) ||
                    (elem is KtParameter && elem.isLoopParameter && elem.typeReference == null) ||
                    (elem is KtDestructuringDeclarationEntry && elem.getReturnTypeReference() == null)
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

        override fun isApplicable(elem: PsiElement): Boolean =
            elem is KtNamedFunction && !(elem.hasBlockBody() || elem.hasDeclaredReturnType())
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
            val callElement = elem.getStrictParentOfType<KtCallElement>() ?: return emptyList()
            return provideArgumentNameHints(callElement)
        }

        override fun isApplicable(elem: PsiElement): Boolean = elem is KtValueArgumentList
    },


    LAMBDA_RETURN_EXPRESSION("Show lambda return expression hints", true) {
        override fun isApplicable(elem: PsiElement) =
            elem is KtExpression && elem !is KtLambdaExpression && elem !is KtFunctionLiteral &&
                    !elem.isNameReferenceInCall()

        override fun provideHints(elem: PsiElement): List<InlayInfo> {
            if (elem is KtExpression) {
                return provideLambdaReturnValueHints(elem)
            }
            return emptyList()
        }
    },

    LAMBDA_IMPLICIT_PARAMETER_RECEIVER("Show hints for implicit receivers and parameters of lambdas", true) {
        override fun isApplicable(elem: PsiElement) = elem is KtLambdaExpression

        override fun provideHints(elem: PsiElement): List<InlayInfo> {
            (elem as? KtLambdaExpression)?.let {
                return provideLambdaImplicitHints(elem)
            }
            return emptyList()
        }
    },
    SUSPENDING_CALL("Show hints for suspending calls", false) {
        override fun isApplicable(elem: PsiElement) = elem.isNameReferenceInCall() && ApplicationManager.getApplication().isInternal

        override fun provideHints(elem: PsiElement): List<InlayInfo> {
            val callExpression = elem.parent as? KtCallExpression ?: return emptyList()
            return provideSuspendingCallHint(callExpression)?.let { listOf(it) } ?: emptyList()
        }
    };

    companion object {

        fun resolve(elem: PsiElement): HintType? = HintType.values().find { it.isApplicable(elem) }
        fun resolveToEnabled(elem: PsiElement?): HintType? {

            val resolved = elem?.let { resolve(it) } ?: return null
            return if (resolved.enabled) {
                resolved
            } else {
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

    override fun getDefaultBlackList(): Set<String> =
        setOf(
            "*listOf", "*setOf", "*arrayOf", "*ListOf", "*SetOf", "*ArrayOf", "*assert*(*)", "*mapOf", "*MapOf",
            "kotlin.require*(*)", "kotlin.check*(*)", "*contains*(value)", "*containsKey(key)", "kotlin.lazyOf(value)",
            "*SequenceBuilder.resume(value)", "*SequenceBuilder.yield(value)"
        )

    override fun getHintInfo(element: PsiElement): HintInfo? {
        val hintType = HintType.resolve(element) ?: return null
        return when (hintType) {
            HintType.PARAMETER_HINT -> {
                val parent = (element as? KtValueArgumentList)?.parent
                (parent as? KtCallElement)?.let { getMethodInfo(it) }
            }
            else -> HintInfo.OptionInfo(hintType.option)
        }
    }

    override fun getParameterHints(element: PsiElement?): List<InlayInfo> =
        HintType.resolveToEnabled(element)?.provideHints(element!!) ?: emptyList()

    override fun getBlackListDependencyLanguage(): Language = JavaLanguage.INSTANCE

    override fun getInlayPresentation(inlayText: String): String =
        if (inlayText.startsWith(TYPE_INFO_PREFIX)) {
            inlayText.substring(TYPE_INFO_PREFIX.length)
        } else {
            super.getInlayPresentation(inlayText)
        }

    private fun getMethodInfo(elem: KtCallElement): HintInfo.MethodInfo? {
        val resolvedCall = elem.resolveToCall()
        val resolvedCallee = resolvedCall?.candidateDescriptor
        if (resolvedCallee is FunctionDescriptor) {
            val paramNames =
                resolvedCallee.valueParameters.asSequence().map { it.name }.filter { !it.isSpecial }.map(Name::asString).toList()
            val fqName = if (resolvedCallee is ConstructorDescriptor)
                resolvedCallee.containingDeclaration.fqNameSafe.asString()
            else
                (resolvedCallee.fqNameOrNull()?.asString() ?: return null)
            return HintInfo.MethodInfo(fqName, paramNames)
        }
        return null
    }
}

private fun PsiElement.isNameReferenceInCall() =
    this is KtNameReferenceExpression && parent is KtCallExpression
