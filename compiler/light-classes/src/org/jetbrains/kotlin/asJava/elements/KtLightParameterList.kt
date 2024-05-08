/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction

class KtLightParameterList(
    private val parent: KtLightMethod,
    private val parametersCount: Int,
    computeParameters: () -> List<PsiParameter>
) : KtLightElementBase(parent), PsiParameterList {

    override val kotlinOrigin: KtElement?
        get() = (parent.kotlinOrigin as? KtFunction)?.valueParameterList

    private val _parameters: Array<PsiParameter> by lazyPub { computeParameters().toTypedArray() }
    override fun getParameters() = _parameters

    override fun getParameterIndex(parameter: PsiParameter) = _parameters.indexOf(parameter)

    override fun getParametersCount() = parametersCount

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitParameterList(this)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KtLightParameterList
        if (parent != other.parent) return false
        return true
    }

    override fun hashCode(): Int = parent.hashCode()

}
