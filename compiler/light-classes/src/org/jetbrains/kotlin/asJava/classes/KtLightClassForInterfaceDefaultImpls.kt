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
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject

class KtLightClassForInterfaceDefaultImpls(
        classFqName: FqName,
        classOrObject: KtClassOrObject)
: KtLightClassForSourceDeclaration({ classFqName }, classOrObject){
    override fun getQualifiedName(): String = classFqName.asString()

    override fun getParent() = containingClass

    override fun copy(): PsiElement {
        return KtLightClassForInterfaceDefaultImpls(classFqName, classOrObject.copy() as KtClassOrObject)
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
