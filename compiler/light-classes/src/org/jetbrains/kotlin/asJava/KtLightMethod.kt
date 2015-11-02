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

import com.intellij.core.JavaCoreBundle
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightMethod
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.*
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*

public interface KtLightMethod : PsiMethod, KtLightElement<KtDeclaration, PsiMethod>

sealed class KtLightMethodImpl(
        private val delegate: PsiMethod,
        private val origin: KtDeclaration?,
        containingClass: KtLightClass
): LightMethod(delegate.manager, delegate, containingClass), KtLightMethod {
    override fun getContainingClass(): KtLightClass = super.getContainingClass() as KtLightClass

    private val paramsList: CachedValue<PsiParameterList> by lazy {
        val cacheManager = CachedValuesManager.getManager(delegate.project)
        cacheManager.createCachedValue<PsiParameterList>({
            val parameterBuilder = LightParameterListBuilder(manager, KotlinLanguage.INSTANCE, this)

            for ((index, parameter) in delegate.parameterList.parameters.withIndex()) {
                parameterBuilder.addParameter(KtLightParameter(parameter, index, this))
            }

            CachedValueProvider.Result.create(parameterBuilder, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
        }, false)
    }

    private val typeParamsList: CachedValue<PsiTypeParameterList> by lazy {
        val cacheManager = CachedValuesManager.getManager(delegate.project)
        cacheManager.createCachedValue<PsiTypeParameterList>({
            val list = if (origin is KtClassOrObject) {
                KotlinLightTypeParameterListBuilder(manager)
            }
            else if (origin == null) {
                delegate.typeParameterList
            }
            else {
                LightClassUtil.buildLightTypeParameterList(this@KtLightMethodImpl, origin)
            }
            CachedValueProvider.Result.create(list, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
        }, false)
    }

    override fun getNavigationElement(): PsiElement = origin ?: super.getNavigationElement()
    override fun getOriginalElement(): PsiElement = origin ?: super.getOriginalElement()
    override fun getDelegate() = delegate
    override fun getOrigin() = origin
    override fun getParent(): PsiElement? = containingClass

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitMethod(this)
        }
        else {
            visitor.visitElement(this)
        }
    }

    override fun setName(name: String): PsiElement? {
        val toRename = origin as? PsiNamedElement ?: throwCanNotModify()
        toRename.setName(name)
        return this
    }

    public override fun delete() {
        origin?.let {
            if (it.isValid) {
                it.delete()
            }
        } ?: throwCanNotModify()
    }

    private fun throwCanNotModify(): Nothing {
        throw IncorrectOperationException(JavaCoreBundle.message("psi.error.attempt.to.edit.class.file"))
    }

    override fun getParameterList() = paramsList.value

    override fun getTypeParameterList() = typeParamsList.value

    override fun getTypeParameters(): Array<PsiTypeParameter> =
            typeParameterList?.let { it.typeParameters } ?: PsiTypeParameter.EMPTY_ARRAY

    override fun getSignature(substitutor: PsiSubstitutor): MethodSignature {
        if (substitutor == PsiSubstitutor.EMPTY) {
            return delegate.getSignature(substitutor)
        }
        return MethodSignatureBackedByPsiMethod.create(this, substitutor)
    }

    override fun copy(): PsiElement {
        return Factory.create(delegate, origin?.copy() as? KtDeclaration, containingClass)
    }

    override fun getUseScope() = origin?.useScope ?: super.getUseScope()

    override fun getLanguage() = KotlinLanguage.INSTANCE

    override fun processDeclarations(processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement?, place: PsiElement): Boolean {
        return typeParameters.all { processor.execute(it, state) }
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (another is KtLightMethod && origin == another.getOrigin() && delegate == another.getDelegate()) {
            return true
        }

        return super.isEquivalentTo(another)
    }

    override fun equals(other: Any?): Boolean =
            other is KtLightMethod &&
            name == other.name &&
            origin == other.getOrigin() &&
            containingClass == other.containingClass &&
            delegate == other.getDelegate()

    override fun hashCode(): Int = ((name.hashCode() * 31 + (origin?.hashCode() ?: 0)) * 31 + containingClass.hashCode()) * 31 + delegate.hashCode()

    override fun toString(): String = "${this.javaClass.simpleName}:$name"

    private class KtLightMethodForDeclaration(
            delegate: PsiMethod, origin: KtDeclaration?, containingClass: KtLightClass
    ) : KtLightMethodImpl(delegate, origin, containingClass)

    private class KtLightAnnotationMethod(
            delegate: PsiAnnotationMethod,
            origin: KtDeclaration?,
            containingClass: KtLightClass
    ) : KtLightMethodImpl(delegate, origin, containingClass), PsiAnnotationMethod {
        override fun getDefaultValue() = getDelegate().defaultValue
        override fun getDelegate() = super.getDelegate() as PsiAnnotationMethod
    }

    companion object Factory {
        fun create(
                delegate: PsiMethod, origin: KtDeclaration?, containingClass: KtLightClass
        ): KtLightMethodImpl {
            return when (delegate) {
                is PsiAnnotationMethod -> KtLightAnnotationMethod(delegate, origin, containingClass)
                else -> KtLightMethodForDeclaration(delegate, origin, containingClass)
            }
        }
    }
}

fun KtLightMethod.isTraitFakeOverride(): Boolean {
    val methodOrigin = this.getOrigin()
    if (!(methodOrigin is KtNamedFunction || methodOrigin is KtPropertyAccessor || methodOrigin is KtProperty)) {
        return false
    }

    val parentOfMethodOrigin = PsiTreeUtil.getParentOfType(methodOrigin, KtClassOrObject::class.java)
    val thisClassDeclaration = (this.containingClass as KtLightClass).getOrigin()

    // Method was generated from declaration in some other trait
    return (parentOfMethodOrigin != null && thisClassDeclaration !== parentOfMethodOrigin && KtPsiUtil.isTrait(parentOfMethodOrigin))
}
