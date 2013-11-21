/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.asJava

import com.intellij.psi.impl.light.LightMethod
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.jet.lang.psi.JetDeclaration
import com.intellij.psi.PsiParameterList
import org.jetbrains.jet.plugin.JetLanguage
import kotlin.properties.Delegates
import org.jetbrains.jet.asJava.light.LightParameter
import org.jetbrains.jet.asJava.light.LightParameterListBuilder
import org.jetbrains.jet.lang.resolve.java.jetAsJava.KotlinLightMethod
import com.intellij.psi.PsiParameterList
import org.jetbrains.jet.plugin.JetLanguage
import kotlin.properties.Delegates
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.openapi.application.Result
import com.intellij.psi.util.CachedValue

public class KotlinLightMethodForDeclaration(manager: PsiManager, val method: PsiMethod, val jetDeclaration: JetDeclaration, containingClass: PsiClass):
        LightMethod(manager, method, containingClass), KotlinLightMethod {

    private val paramsList: CachedValue<PsiParameterList> by Delegates.blockingLazy {
        val cacheManager = CachedValuesManager.getManager(method.getProject())
        cacheManager.createCachedValue<PsiParameterList>({
            val parameterBuilder = LightParameterListBuilder(getManager(), JetLanguage.INSTANCE)

            for ((index, parameter) in method.getParameterList().getParameters().withIndices()) {
                val lightParameter = LightParameter(parameter.getName() ?: "p$index", parameter.getType(), this, JetLanguage.INSTANCE)
                parameterBuilder.addParameter(lightParameter)
            }

            CachedValueProvider.Result.create(parameterBuilder, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
        }, false)
    }

    override fun getNavigationElement() : PsiElement = jetDeclaration
    override fun getOriginalElement() : PsiElement = jetDeclaration
    override fun getOrigin(): JetDeclaration? = jetDeclaration

    override fun getParent(): PsiElement? = getContainingClass()

    override fun setName(name: String): PsiElement? {
        (jetDeclaration as PsiNamedElement).setName(name)
        return this
    }

    public override fun delete() {
        if (jetDeclaration.isValid()) {
            jetDeclaration.delete()
        }
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (another is KotlinLightMethod && getOrigin() == another.getOrigin()) {
            return true
        }

        return super<LightMethod>.isEquivalentTo(another)
    }

    override fun getParameterList(): PsiParameterList = paramsList.getValue()!!

    override fun copy(): PsiElement? {
        return KotlinLightMethodForDeclaration(getManager()!!, method, jetDeclaration.copy() as JetDeclaration, getContainingClass()!!)
    }
}
