/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtNamedFunction

sealed class CallInfo {
    abstract val isSuspendCall: Boolean
    abstract val targetFunction: PsiElement?
    abstract val isJavaFunctionCall: Boolean
}

data class VariableAsFunctionCallInfo(val target: PsiElement, override val isSuspendCall: Boolean) : CallInfo() {
    override val targetFunction: PsiElement? = null
    override val isJavaFunctionCall: Boolean = false
}

data class VariableAsFunctionLikeCallInfo(val target: PsiElement, val invokeFunction: KtNamedFunction) : CallInfo() {
    override val isSuspendCall: Boolean get() = invokeFunction.hasModifier(KtTokens.SUSPEND_KEYWORD)
    override val targetFunction: PsiElement? get() = invokeFunction
    override val isJavaFunctionCall: Boolean = false
}

// SimpleFunctionCallInfo

sealed class SimpleFunctionCallInfo : CallInfo() {
    abstract override val targetFunction: PsiElement
}

data class SimpleKtFunctionCallInfo(override val targetFunction: KtNamedFunction) : SimpleFunctionCallInfo() {
    override val isSuspendCall: Boolean get() = targetFunction.hasModifier(KtTokens.SUSPEND_KEYWORD)
    override val isJavaFunctionCall: Boolean = true
}

data class SimpleJavaFunctionCallInfo(override val targetFunction: PsiMethod) : SimpleFunctionCallInfo() {
    override val isSuspendCall: Boolean = false
    override val isJavaFunctionCall: Boolean = false
}


// ConstructorCallInfo

sealed class ConstructorCallInfo : CallInfo() {
    final override val isSuspendCall: Boolean = false
    abstract val isPrimary: Boolean
}

data class KtExplicitConstructorCallInfo(
    override val targetFunction: KtConstructor<*>,
    override val isPrimary: Boolean
) : ConstructorCallInfo() {
    override val isJavaFunctionCall: Boolean = false
}

data class KtImplicitPrimaryConstructorCallInfo(val owner: KtClass) : ConstructorCallInfo() {
    override val targetFunction: PsiElement? = null
    override val isJavaFunctionCall: Boolean = false
    override val isPrimary: Boolean = true
}

data class JavaExplicitConstructorCallInfo(
    override val targetFunction: PsiMethod,
    override val isPrimary: Boolean
) : ConstructorCallInfo() {
    override val isJavaFunctionCall: Boolean = true
}

data class JavaImplicitPrimaryConstructorCallInfo(val owner: PsiClass) : ConstructorCallInfo() {
    override val targetFunction: PsiElement? = null
    override val isJavaFunctionCall: Boolean = true
    override val isPrimary: Boolean = true
}
