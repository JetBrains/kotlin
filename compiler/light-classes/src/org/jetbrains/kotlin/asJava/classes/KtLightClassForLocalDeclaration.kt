/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.light.LightClass
import com.intellij.psi.impl.light.LightMethod
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

open class KtLightClassForLocalDeclaration(
    classOrObject: KtClassOrObject
) : KtLightClassForSourceDeclaration(classOrObject) {
    override val myInnersCache: KotlinClassInnerStuffCache =
        KotlinClassInnerStuffCache(
            this,
            with(KotlinModificationTrackerService.getInstance(classOrObject.project)) {
                val file = classOrObject.containingFile
                if (file is KtFile) {
                    listOf(outOfBlockModificationTracker, fileModificationTracker(file))
                } else {
                    listOf(outOfBlockModificationTracker)
                }
            }
        )

    override fun copy(): PsiElement = KtLightClassForLocalDeclaration(classOrObject.copy() as KtClassOrObject)

    override fun getQualifiedName(): String? = null

    override fun getParent() = _parent

    private val _parent: PsiElement? by lazyPub { getParentForLocalDeclaration(classOrObject) }

    companion object {
        fun getParentForLocalDeclaration(classOrObject: KtClassOrObject): PsiElement? {

            fun getParentByPsiMethod(method: PsiMethod?, name: String?, forceMethodWrapping: Boolean): PsiElement? {
                if (method == null || name == null) return null

                var containingClass: PsiClass? = method.containingClass ?: return null

                val currentFileName = classOrObject.containingFile.name

                var createWrapper = forceMethodWrapping
                // Use PsiClass wrapper instead of package light class to avoid names like "FooPackage" in Type Hierarchy and related views
                if (containingClass is KtLightClassForFacade) {
                    containingClass = object : LightClass(containingClass as KtLightClassForFacade, KotlinLanguage.INSTANCE) {
                        override fun getName(): String? = currentFileName
                    }
                    createWrapper = true
                }

                if (createWrapper) {
                    return object : LightMethod(classOrObject.manager, method, containingClass!!, KotlinLanguage.INSTANCE) {
                        override fun getParent(): PsiElement = getContainingClass()
                        override fun getName(): String = name
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
                KtParameter::class.java
            )

            if (declaration is KtParameter) {
                declaration = declaration.getStrictParentOfType<KtNamedDeclaration>()
            }

            if (declaration is KtFunction) {
                return getParentByPsiMethod(
                    LightClassUtil.getLightClassMethod(declaration),
                    declaration.name,
                    forceMethodWrapping = false
                )
            }

            // Represent the property as a fake method with the same name
            if (declaration is KtProperty) {
                return getParentByPsiMethod(
                    LightClassUtil.getLightClassPropertyMethods(declaration).getter,
                    declaration.name,
                    forceMethodWrapping = true
                )
            }

            if (declaration is KtAnonymousInitializer) {
                val parent = declaration.parent
                val grandparent = parent.parent

                if (parent is KtClassBody && grandparent is KtClassOrObject) {
                    return grandparent.toLightClass()
                }
            }

            return if (declaration is KtClass) declaration.toLightClass() else null
        }
    }
}