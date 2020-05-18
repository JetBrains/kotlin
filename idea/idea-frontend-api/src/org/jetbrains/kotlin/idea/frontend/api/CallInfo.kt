/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction

sealed class CallInfo {
    abstract val isSuspendCall: Boolean
    abstract val targetFunction: PsiElement?
}

data class VariableAsFunctionCallInfo(val target: PsiElement, override val isSuspendCall: Boolean) : CallInfo() {
    override val targetFunction: PsiElement? = null
}

data class VariableAsFunctionLikeCallInfo(val target: PsiElement, val invokeFunction: KtNamedFunction) : CallInfo() {
    override val isSuspendCall: Boolean get() = invokeFunction.hasModifier(KtTokens.SUSPEND_KEYWORD)
    override val targetFunction: PsiElement? get() = invokeFunction
}

// SimpleFunctionCallInfo

sealed class SimpleFunctionCallInfo : CallInfo() {
    abstract override val targetFunction: PsiElement
}

data class SimpleKtFunctionCallInfo(override val targetFunction: KtNamedFunction) : SimpleFunctionCallInfo() {
    override val isSuspendCall: Boolean get() = targetFunction.hasModifier(KtTokens.SUSPEND_KEYWORD)
}

data class SimpleJavaFunctionCallInfo(override val targetFunction: PsiMethod) : SimpleFunctionCallInfo() {
    override val isSuspendCall: Boolean = false
}


// ConstructorCallInfo

//TODO
object ConstructorCallInfo : CallInfo() {
    //    abstract val targetConstructor: PsiElement?
    final override val isSuspendCall: Boolean = false
    override val targetFunction: PsiElement? = null
}

//data class SimpleKtConstructorCallInfo(override val targetConstructor: KtConstructor<*>) : ConstructorCallInfo()

//data class SimpleJavaConstructorCallInfo(override val targetConstructor: PsiMethod) : ConstructorCallInfo()

