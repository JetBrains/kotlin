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
import org.jetbrains.jet.asJava.light.LightParameter
import org.jetbrains.jet.asJava.light.LightParameterListBuilder
import org.jetbrains.jet.lang.resolve.java.jetAsJava.KotlinLightMethod
import com.intellij.psi.PsiParameterList
import org.jetbrains.jet.plugin.JetLanguage
import kotlin.properties.Delegates
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValue
import com.intellij.psi.PsiTypeParameterList
import org.jetbrains.jet.lang.psi.JetPropertyAccessor
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetProperty
import com.intellij.psi.PsiTypeParameter
import org.jetbrains.jet.lang.psi.JetClassOrObject
import com.intellij.psi.impl.light.LightTypeParameterListBuilder
import com.intellij.psi.search.SearchScope
import org.jetbrains.jet.utils.*

public class KotlinLightMethodForDeclaration(
        manager: PsiManager, override val delegate: PsiMethod, override val origin: JetDeclaration, containingClass: PsiClass
): LightMethod(manager, delegate, containingClass), KotlinLightMethod {

    private val paramsList: CachedValue<PsiParameterList> by Delegates.blockingLazy {
        val cacheManager = CachedValuesManager.getManager(delegate.getProject())
        cacheManager.createCachedValue<PsiParameterList>({
            val parameterBuilder = LightParameterListBuilder(getManager(), JetLanguage.INSTANCE)

            for ((index, parameter) in delegate.getParameterList().getParameters().withIndices()) {
                parameterBuilder.addParameter(KotlinLightParameter(parameter, index, this))
            }

            CachedValueProvider.Result.create(parameterBuilder, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
        }, false)
    }

    private val typeParamsList: CachedValue<PsiTypeParameterList> by Delegates.blockingLazy {
        val cacheManager = CachedValuesManager.getManager(delegate.getProject())
        cacheManager.createCachedValue<PsiTypeParameterList>({
            val declaration = if (origin is JetPropertyAccessor) origin.getParentByType(javaClass<JetProperty>()) else origin

            val list = if (origin is JetClassOrObject) {
                LightTypeParameterListBuilder(getManager(), getLanguage())
            }
            else {
                LightClassUtil.buildLightTypeParameterList(this@KotlinLightMethodForDeclaration, origin)
            }
            CachedValueProvider.Result.create(list, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
        }, false)
    }

    override fun getNavigationElement() : PsiElement = origin
    override fun getOriginalElement() : PsiElement = origin

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
        if (another is KotlinLightMethod && origin == another.origin) {
            return true
        }

        return super<LightMethod>.isEquivalentTo(another)
    }

    override fun getParameterList(): PsiParameterList = paramsList.getValue()!!

    override fun getTypeParameterList(): PsiTypeParameterList? = typeParamsList.getValue()
    override fun getTypeParameters(): Array<PsiTypeParameter> =
            getTypeParameterList()?.let { it.getTypeParameters() } ?: PsiTypeParameter.EMPTY_ARRAY

    override fun copy(): PsiElement {
        return KotlinLightMethodForDeclaration(getManager()!!, delegate, origin.copy() as JetDeclaration, getContainingClass()!!)
    }

    override fun getUseScope(): SearchScope = origin.getUseScope()

    override fun equals(other: Any?): Boolean =
            other is KotlinLightMethodForDeclaration && getName() == other.getName() && origin == other.origin

    override fun hashCode(): Int = getName().hashCode() * 31 + origin.hashCode()
}
