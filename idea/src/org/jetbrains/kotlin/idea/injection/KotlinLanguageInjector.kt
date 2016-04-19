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

package org.jetbrains.kotlin.idea.injection

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.intelliLang.Configuration
import org.intellij.plugins.intelliLang.inject.InjectorUtils
import org.intellij.plugins.intelliLang.inject.java.JavaLanguageInjectionSupport
import org.jetbrains.kotlin.psi.*
import java.util.*

class KotlinLanguageInjector : LanguageInjector {
    val kotlinSupport: KotlinLanguageInjectionSupport? by lazy {
        ArrayList(InjectorUtils.getActiveInjectionSupports()).filterIsInstance(KotlinLanguageInjectionSupport::class.java).firstOrNull()
    }

    override fun getLanguagesToInject(host: PsiLanguageInjectionHost, injectionPlacesRegistrar: InjectedLanguagePlaces) {
        if (!host.isValidHost) return
        val ktHost: KtElement = host as? KtElement ?: return

        val injectionInfo =
                injectWithExplicitCodeInstruction(ktHost)
                ?: injectWithCall(ktHost)
                ?: return

        val language = InjectorUtils.getLanguageByString(injectionInfo.languageId) ?: return
        injectionPlacesRegistrar.addPlace(language, TextRange.from(0, ktHost.textLength), injectionInfo.prefix, injectionInfo.suffix)
    }

    private fun injectWithExplicitCodeInstruction(host: KtElement): InjectionInfo? {
        val support = kotlinSupport ?: return null
        val languageId = support.findAnnotationInjectionLanguageId(host) ?: return null
        return InjectionInfo(languageId, null, null)
    }

    private fun injectWithCall(host: KtElement): InjectionInfo? {
        val ktHost: KtElement = host
        val argument = ktHost.parent as? KtValueArgument ?: return null

        val callExpression = PsiTreeUtil.getParentOfType(ktHost, KtCallExpression::class.java) ?: return null
        val callee = callExpression.calleeExpression ?: return null

        for (reference in callee.references) {
            ProgressManager.checkCanceled()

            val resolvedTo = reference.resolve()
            if (resolvedTo is PsiMethod) {
                val injectionForJavaMethod = injectionForJavaMethod(argument, resolvedTo)
                if (injectionForJavaMethod != null) {
                    return injectionForJavaMethod
                }
            }
            else if (resolvedTo is KtFunction) {
                val injectionForJavaMethod = injectionForKotlinCall(argument, resolvedTo)
                if (injectionForJavaMethod != null) {
                    return injectionForJavaMethod
                }
            }
        }

        return null
    }

    private fun injectionForJavaMethod(argument: KtValueArgument, javaMethod: PsiMethod): InjectionInfo? {
        val argumentIndex = (argument.parent as KtValueArgumentList).arguments.indexOf(argument)
        val psiParameter = javaMethod.parameterList.parameters.getOrNull(argumentIndex) ?: return null

        val injections = Configuration.getInstance().getInjections(JavaLanguageInjectionSupport.JAVA_SUPPORT_ID)

        for (injection in injections) {
            if (injection.acceptsPsiElement(psiParameter)) {
                // ?? if (!processXmlInjections(injection, owner, method, paramIndex)) {
                return InjectionInfo(injection.injectedLanguageId, injection.prefix, injection.suffix)
            }
        }

        return null
    }

    private fun injectionForKotlinCall(argument: KtValueArgument, ktFunction: KtFunction): InjectionInfo? {
        val argumentIndex = (argument.parent as KtValueArgumentList).arguments.indexOf(argument)
        val ktParameter = ktFunction.valueParameters.getOrNull(argumentIndex) ?: return null

        val injections = Configuration.getInstance().getInjections(KOTLIN_SUPPORT_ID)

        for (injection in injections) {
            if (injection.acceptsPsiElement(ktParameter)) {
                return InjectionInfo(injection.injectedLanguageId, injection.prefix, injection.suffix)
            }
        }

        return null
    }

    private class InjectionInfo(val languageId: String?, val prefix: String?, val suffix: String?)
}