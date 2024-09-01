/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.light.LightClass
import com.intellij.psi.impl.light.LightField
import com.intellij.psi.impl.light.LightMethod
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents

fun getParentForLocalDeclaration(classOrObject: KtClassOrObject): PsiElement? {
    fun <T : PsiMember> wrapMember(member: T, forceWrapping: Boolean, wrapper: (T, PsiClass) -> T): T? {
        val containingClass = member.containingClass ?: return null

        if (containingClass is KtLightClassForFacade) {
            val facadeClassName = classOrObject.containingFile.name
            val wrappedClass = object : LightClass(containingClass, KotlinLanguage.INSTANCE) {
                override fun getName(): String = facadeClassName
            }

            return wrapper(member, wrappedClass)
        }

        return if (forceWrapping) wrapper(member, containingClass) else member
    }

    fun wrapMethod(member: PsiMethod, name: String = member.name, forceWrapping: Boolean): PsiMethod? {
        return wrapMember(member, forceWrapping) { method, containingClass ->
            object : LightMethod(containingClass.manager, method, containingClass, KotlinLanguage.INSTANCE) {
                override fun getParent(): PsiElement = containingClass
                override fun getName(): String = name
            }
        }
    }

    fun wrapField(member: PsiField, forceWrapping: Boolean): PsiField? {
        return wrapMember(member, forceWrapping) { field, containingClass ->
            object : LightField(containingClass.manager, field, containingClass) {
                override fun getParent(): PsiElement = containingClass
            }
        }
    }

    fun map(declaration: KtElement): PsiElement? {
        when (declaration) {
            is KtFunction -> {
                val psiMethod = LightClassUtil.getLightClassMethod(declaration)
                if (psiMethod != null) {
                    return wrapMethod(psiMethod, declaration.name ?: psiMethod.name, forceWrapping = false)
                }
            }

            is KtPropertyAccessor -> {
                val psiMethod = LightClassUtil.getLightClassAccessorMethod(declaration)
                if (psiMethod != null) {
                    return wrapMethod(psiMethod, forceWrapping = true)
                }
            }

            is KtProperty -> {
                if (!declaration.isLocal) {
                    val propertyMethods = LightClassUtil.getLightClassPropertyMethods(declaration)
                    val psiMethod = propertyMethods.getter
                    if (psiMethod != null) {
                        return wrapMethod(psiMethod, forceWrapping = true)
                    }

                    val psiField = propertyMethods.backingField
                    if (psiField != null) {
                        return wrapField(psiField, forceWrapping = false)
                    }
                }
            }

            is KtAnonymousInitializer -> {
                val parent = declaration.parent
                val grandparent = parent.parent

                if (parent is KtClassBody && grandparent is KtClassOrObject) {
                    return grandparent.toLightClass()
                }
            }

            is KtClass -> {
                return declaration.toLightClass()
            }
        }

        return null
    }

    return classOrObject.parents
        .filterIsInstance<KtElement>()
        .firstNotNullOfOrNull { map(it) }
}