/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

private val ktTokenToPsiModifier = listOf(
    KtTokens.PUBLIC_KEYWORD to PsiModifier.PUBLIC,
    KtTokens.INTERNAL_KEYWORD to PsiModifier.PUBLIC,
    KtTokens.PROTECTED_KEYWORD to PsiModifier.PROTECTED,
)

fun KtClassOrObject.computeModifiersByPsi(
    hasAbstractMember: (KtClassOrObject) -> Boolean,
): Set<String> {
    val psiModifiers = hashSetOf<String>()

    // PUBLIC, PROTECTED, PRIVATE
    //noinspection unchecked

    for (tokenAndModifier in ktTokenToPsiModifier) {
        if (hasModifier(tokenAndModifier.first)) {
            psiModifiers.add(tokenAndModifier.second)
        }
    }

    if (hasModifier(KtTokens.PRIVATE_KEYWORD)) {
        // Top-level private class has PACKAGE_LOCAL visibility in Java
        // Nested private class has PRIVATE visibility
        psiModifiers.add(if (isTopLevel()) PsiModifier.PACKAGE_LOCAL else PsiModifier.PRIVATE)
    } else if (!psiModifiers.contains(PsiModifier.PROTECTED)) {
        psiModifiers.add(PsiModifier.PUBLIC)
    }

    // ABSTRACT
    if (isAbstract(hasAbstractMember) || isSealed) {
        psiModifiers.add(PsiModifier.ABSTRACT)
    }

    // STATIC
    if (!isTopLevel() && !hasModifier(KtTokens.INNER_KEYWORD)) {
        psiModifiers.add(PsiModifier.STATIC)
    }

    return psiModifiers
}

private fun KtClassOrObject.isAbstract(
    hasAbstractMember: (KtClassOrObject) -> Boolean,
): Boolean =
    hasModifier(KtTokens.ABSTRACT_KEYWORD) || isInterface || (isEnum && hasAbstractMember(this))

private val KtClassOrObject.isInterface: Boolean
    get() {
        if (this !is KtClass) return false
        return isInterface() || isAnnotation()
    }

private val KtClassOrObject.isEnum: Boolean
    get() = this is KtClass && isEnum()

private val KtClassOrObject.isSealed: Boolean
    get() = hasModifier(KtTokens.SEALED_KEYWORD)
