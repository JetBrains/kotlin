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

package org.jetbrains.kotlin.idea

import com.intellij.ide.IconProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.ui.RowIcon
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import javax.swing.Icon

class KotlinIconProvider : IconProvider(), DumbAware {

    override fun getIcon(psiElement: PsiElement, flags: Int): Icon? {
        if (psiElement is KtFile) {
            val mainClass = getMainClass(psiElement)
            return if (mainClass != null && psiElement.declarations.size == 1) getIcon(mainClass, flags) else KotlinIcons.FILE
        }

        val result = psiElement.getBaseIcon()
        if (flags and Iconable.ICON_FLAG_VISIBILITY > 0 && result != null && (psiElement is KtModifierListOwner && psiElement !is KtClassInitializer)) {
            val list = psiElement.modifierList
            return createRowIcon(result, getVisibilityIcon(list))
        }
        return result
    }

    companion object {

        var INSTANCE = KotlinIconProvider()

        fun getMainClass(file: KtFile): KtClassOrObject? {
            val classes = file.declarations.filterIsInstance<KtClassOrObject>()
            if (classes.size == 1 && StringUtil.getPackageName(file.name) == classes[0].name) {
                return classes[0]
            }
            return null
        }

        private fun createRowIcon(baseIcon: Icon, visibilityIcon: Icon): RowIcon {
            val rowIcon = RowIcon(2)
            rowIcon.setIcon(baseIcon, 0)
            rowIcon.setIcon(visibilityIcon, 1)
            return rowIcon
        }

        fun getVisibilityIcon(list: KtModifierList?): Icon {
            if (list != null) {
                if (list.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
                    return PlatformIcons.PRIVATE_ICON
                }
                if (list.hasModifier(KtTokens.PROTECTED_KEYWORD)) {
                    return PlatformIcons.PROTECTED_ICON
                }
                if (list.hasModifier(KtTokens.INTERNAL_KEYWORD)) {
                    return PlatformIcons.PACKAGE_LOCAL_ICON
                }
            }

            return PlatformIcons.PUBLIC_ICON
        }

        fun PsiElement.getBaseIcon(): Icon? = when(this) {
            is KtPackageDirective -> PlatformIcons.PACKAGE_ICON
            is KtLightClassForFacade -> KotlinIcons.FILE
            is KtLightClassForDecompiledDeclaration -> {
                val origin = kotlinOrigin
                //TODO (light classes for decompiled files): correct presentation
                if (origin != null) origin.getBaseIcon() else KotlinIcons.CLASS
            }
            is KtLightClassForSourceDeclaration -> navigationElement.getBaseIcon()
            is KtNamedFunction -> when {
                receiverTypeReference != null -> KotlinIcons.EXTENSION_FUNCTION
                getStrictParentOfType<KtNamedDeclaration>() is KtClass ->
                    if (KtPsiUtil.isAbstract(this)) PlatformIcons.ABSTRACT_METHOD_ICON else PlatformIcons.METHOD_ICON
                else -> KotlinIcons.FUNCTION
            }
            is KtFunctionLiteral -> KotlinIcons.LAMBDA
            is KtClass -> when {
                isInterface() -> KotlinIcons.INTERFACE
                isEnum() -> KotlinIcons.ENUM
                isAnnotation() -> KotlinIcons.ANNOTATION
                this is KtEnumEntry && getPrimaryConstructorParameterList() == null -> KotlinIcons.ENUM
                else -> KotlinIcons.CLASS
            }
            is KtObjectDeclaration -> KotlinIcons.OBJECT
            is KtParameter -> {
                if (KtPsiUtil.getClassIfParameterIsProperty(this) != null) {
                    if (isMutable) KotlinIcons.FIELD_VAR else KotlinIcons.FIELD_VAL
                }
                else
                    KotlinIcons.PARAMETER
            }
            is KtProperty -> if (isVar) KotlinIcons.FIELD_VAR else KotlinIcons.FIELD_VAL
            is KtClassInitializer -> KotlinIcons.CLASS_INITIALIZER
            is KtTypeAlias -> KotlinIcons.TYPE_ALIAS
            else -> null
        }
    }
}
