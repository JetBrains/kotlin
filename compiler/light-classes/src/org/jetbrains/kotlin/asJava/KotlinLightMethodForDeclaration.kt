/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava

import com.intellij.psi.impl.light.LightMethod
import com.intellij.psi.*
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.idea.JetLanguage
import kotlin.properties.Delegates
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValue
import org.jetbrains.kotlin.psi.JetClassOrObject
import com.intellij.psi.search.SearchScope
import com.intellij.lang.Language
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod

open public class KotlinLightMethodForDeclaration(
        manager: PsiManager,
        private val delegate: PsiMethod,
        private val origin: JetDeclaration,
        containingClass: PsiClass
): LightMethod(manager, delegate, containingClass), KotlinLightMethod {

    private val paramsList: CachedValue<PsiParameterList> by Delegates.blockingLazy {
        val cacheManager = CachedValuesManager.getManager(delegate.getProject())
        cacheManager.createCachedValue<PsiParameterList>({
            val parameterBuilder = LightParameterListBuilder(getManager(), JetLanguage.INSTANCE, this)

            for ((index, parameter) in delegate.getParameterList().getParameters().withIndex()) {
                parameterBuilder.addParameter(KotlinLightParameter(parameter, index, this))
            }

            CachedValueProvider.Result.create(parameterBuilder, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
        }, false)
    }

    private val typeParamsList: CachedValue<PsiTypeParameterList> by Delegates.blockingLazy {
        val cacheManager = CachedValuesManager.getManager(delegate.getProject())
        cacheManager.createCachedValue<PsiTypeParameterList>({
            val list = if (origin is JetClassOrObject) {
                KotlinLightTypeParameterListBuilder(getManager())
            }
            else {
                LightClassUtil.buildLightTypeParameterList(this@KotlinLightMethodForDeclaration, origin)
            }
            CachedValueProvider.Result.create(list, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
        }, false)
    }

    override fun getNavigationElement(): PsiElement = origin
    override fun getOriginalElement(): PsiElement = origin

    override fun getDelegate(): PsiMethod = delegate

    override fun getOrigin(): JetDeclaration = origin

    override fun getParent(): PsiElement? = getContainingClass()

    override fun setName(name: String): PsiElement? {
        (origin as PsiNamedElement).setName(name)
        return this
    }

    public override fun delete() {
        if (origin.isValid()) {
            origin.delete()
        }
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (another is KotlinLightMethod && origin == another.getOrigin()) {
            return true
        }

        return super<LightMethod>.isEquivalentTo(another)
    }

    override fun getParameterList(): PsiParameterList = paramsList.getValue()!!

    override fun getTypeParameterList(): PsiTypeParameterList? = typeParamsList.getValue()
    override fun getTypeParameters(): Array<PsiTypeParameter> =
            getTypeParameterList()?.let { it.getTypeParameters() } ?: PsiTypeParameter.EMPTY_ARRAY

    override fun getSignature(substitutor: PsiSubstitutor): MethodSignature {
        if (substitutor == PsiSubstitutor.EMPTY) {
            return delegate.getSignature(substitutor)
        }
        return MethodSignatureBackedByPsiMethod.create(this, substitutor)
    }

    override fun copy(): PsiElement {
        return KotlinLightMethodForDeclaration(getManager()!!, delegate, origin.copy() as JetDeclaration, getContainingClass()!!)
    }

    override fun getUseScope(): SearchScope = origin.getUseScope()

    override fun getLanguage(): Language = JetLanguage.INSTANCE

    override fun processDeclarations(processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement?, place: PsiElement): Boolean {
        return getTypeParameters().all { processor.execute(it, state) }
    }

    override fun equals(other: Any?): Boolean =
            other is KotlinLightMethodForDeclaration &&
            getName() == other.getName() &&
            origin == other.origin &&
            getContainingClass() == other.getContainingClass()

    override fun hashCode(): Int = (getName().hashCode() * 31 + origin.hashCode()) * 31 + getContainingClass()!!.hashCode()
}
