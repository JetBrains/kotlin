/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiFieldImpl
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAbstract
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors as Colors

internal fun textAttributesKeyForPropertyDeclaration(declaration: PsiElement): TextAttributesKey? = when {
    declaration is KtProperty && declaration.isExtensionDeclaration() -> Colors.EXTENSION_PROPERTY
    declaration is KtProperty && declaration.isLocal || declaration is PsiLocalVariable ->
        Colors.LOCAL_VARIABLE
    declaration is KtParameter -> {
        if (declaration.valOrVarKeyword != null) Colors.INSTANCE_PROPERTY
        else Colors.PARAMETER
    }
    declaration is PsiParameter -> Colors.PARAMETER
    declaration is KtProperty && declaration.isTopLevel -> {
        if (declaration.isCustomPropertyDeclaration()) Colors.PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION
        else Colors.PACKAGE_PROPERTY
    }
    declaration is KtProperty -> {
        if (declaration.isCustomPropertyDeclaration()) Colors.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION
        else Colors.INSTANCE_PROPERTY
    }
    declaration is PsiField -> Colors.INSTANCE_PROPERTY
    else -> null
}

private fun KtProperty.isCustomPropertyDeclaration() =
    getter?.bodyExpression != null || setter?.bodyExpression != null

@Suppress("UnstableApiUsage")
internal fun textAttributesKeyForTypeDeclaration(declaration: PsiElement): TextAttributesKey? = when {
    declaration is KtTypeParameter || declaration is PsiTypeParameter -> Colors.TYPE_PARAMETER
    declaration is KtTypeAlias -> Colors.TYPE_ALIAS
    declaration is KtClass && declaration.isInterface()
            || declaration is PsiClass && declaration.isInterface && !declaration.isAnnotationType -> Colors.TRAIT
    declaration.isAnnotationClass() -> Colors.ANNOTATION
    declaration is KtObjectDeclaration -> Colors.OBJECT
    declaration is KtEnumEntry || declaration is PsiEnumConstant -> Colors.ENUM_ENTRY
    declaration is KtClass && declaration.isAbstract()
            || declaration is PsiClass && declaration.hasModifier(JvmModifier.ABSTRACT) -> Colors.ABSTRACT_CLASS
    declaration is KtClass || declaration is PsiClass -> Colors.CLASS
    else -> null
}

internal fun PsiElement.isAnnotationClass() =
    this is KtClass && isAnnotation() || this is PsiClass && isAnnotationType