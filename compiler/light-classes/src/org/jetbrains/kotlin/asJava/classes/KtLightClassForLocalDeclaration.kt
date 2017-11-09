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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.light.LightClass
import com.intellij.psi.impl.light.LightMethod
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

open class KtLightClassForLocalDeclaration(
        classOrObject: KtClassOrObject
) : KtLightClassForSourceDeclaration(classOrObject) {

    override fun copy(): PsiElement = KtLightClassForLocalDeclaration(classOrObject.copy() as KtClassOrObject)
    override fun getQualifiedName(): String? = null

    override fun getParent() = _parent

    private val _parent: PsiElement? by lazyPub(this::computeParent)

    private fun computeParent(): PsiElement? {
        fun getParentByPsiMethod(method: PsiMethod?, name: String?, forceMethodWrapping: Boolean): PsiElement? {
            if (method == null || name == null) return null

            var containingClass: PsiClass? = method.containingClass ?: return null

            val currentFileName = classOrObject.containingFile.name

            var createWrapper = forceMethodWrapping
            // Use PsiClass wrapper instead of package light class to avoid names like "FooPackage" in Type Hierarchy and related views
            if (containingClass is KtLightClassForFacade) {
                containingClass = object : LightClass(containingClass as KtLightClassForFacade, KotlinLanguage.INSTANCE) {
                    override fun getName(): String? {
                        return currentFileName
                    }
                }
                createWrapper = true
            }

            if (createWrapper) {
                return object : LightMethod(myManager, method, containingClass!!, KotlinLanguage.INSTANCE) {
                    override fun getParent(): PsiElement {
                        return getContainingClass()
                    }

                    override fun getName(): String {
                        return name
                    }
                }
            }

            return method
        }

        var declaration: PsiElement? = KtPsiUtil.getTopmostParentOfTypes(
                classOrObject,
                KtNamedFunction::class.java,
                KtConstructor::class.java,
                KtProperty::class.java,
                KtAnonymousInitializer::class.java,
                KtParameter::class.java)

        if (declaration is KtParameter) {
            declaration = declaration.getStrictParentOfType<KtNamedDeclaration>()
        }

        if (declaration is KtFunction) {
            return getParentByPsiMethod(LightClassUtil.getLightClassMethod(declaration), declaration.name, false)
        }

        // Represent the property as a fake method with the same name
        if (declaration is KtProperty) {
            return getParentByPsiMethod(LightClassUtil.getLightClassPropertyMethods(declaration).getter, declaration.name, true)
        }

        if (declaration is KtAnonymousInitializer) {
            val parent = declaration.parent
            val grandparent = parent.parent

            if (parent is KtClassBody && grandparent is KtClassOrObject) {
                return grandparent.toLightClass()
            }
        }

        if (declaration is KtClass) {
            return declaration.toLightClass()
        }
        return null
    }

}