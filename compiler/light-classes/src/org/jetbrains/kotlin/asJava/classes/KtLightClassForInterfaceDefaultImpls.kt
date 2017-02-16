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

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.DebugUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.builder.LightClassData
import org.jetbrains.kotlin.asJava.builder.findDelegate
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtClassOrObject

class KtLightClassForInterfaceDefaultImpls(
        classOrObject: KtClassOrObject)
    : KtLightClassForSourceDeclaration(classOrObject) {
    override fun getQualifiedName(): String? = containingClass?.qualifiedName?.let { it + ".${JvmAbi.DEFAULT_IMPLS_CLASS_NAME}" }

    override fun getName() = JvmAbi.DEFAULT_IMPLS_CLASS_NAME
    override fun getParent() = containingClass

    override fun copy(): PsiElement {
        return KtLightClassForInterfaceDefaultImpls(classOrObject.copy() as KtClassOrObject)
    }

    override fun findLightClassData(): LightClassData {
        return getLightClassDataHolder().findData {
            it.findDelegate(classOrObject).findInnerClassByName(JvmAbi.DEFAULT_IMPLS_CLASS_NAME, false)
            ?: throw IllegalStateException("Couldn't get delegate for $this\n in ${DebugUtil.stubTreeToString(it)}")
        }
    }

    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = emptyArray()

    override fun computeModifiers(): Array<String> = arrayOf(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL)

    override fun isInterface(): Boolean = false
    override fun isDeprecated(): Boolean = false
    override fun isAnnotationType(): Boolean = false
    override fun isEnum(): Boolean = false
    override fun hasTypeParameters(): Boolean = false
    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean = false

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement {
        throw IncorrectOperationException("Impossible to rename DefaultImpls")
    }

    override fun getContainingClass() = KtLightClassForSourceDeclaration.create(classOrObject)

    override fun getOwnInnerClasses() = emptyList<PsiClass>()
}
