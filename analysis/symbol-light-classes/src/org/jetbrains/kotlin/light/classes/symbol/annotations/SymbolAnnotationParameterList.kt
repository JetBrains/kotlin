/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightIdentifier
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedAnnotationValue
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.light.classes.symbol.toAnnotationMemberValue
import org.jetbrains.kotlin.psi.KtElement

internal class SymbolAnnotationParameterList(
    parent: SymbolLightAbstractAnnotation,
    private val arguments: List<KtNamedAnnotationValue>,
) : KtLightElementBase(parent), PsiAnnotationParameterList {

    private val _attributes: Array<PsiNameValuePair> by lazyPub {
        arguments.map {
            SymbolNameValuePairForAnnotationArgument(it, this)
        }.toTypedArray()
    }

    override fun getAttributes(): Array<PsiNameValuePair> = _attributes

    override val kotlinOrigin: KtElement? get() = null

    //TODO: EQ GHC EQIV
}

private class SymbolNameValuePairForAnnotationArgument(
    private val constantValue: KtNamedAnnotationValue,
    parent: PsiElement
) : KtLightElementBase(parent), PsiNameValuePair {

    override val kotlinOrigin: KtElement? get() = null

    private val _value by lazyPub {
        constantValue.expression.toAnnotationMemberValue(this)
    }

    override fun setValue(p0: PsiAnnotationMemberValue) = cannotModify()

    private val _nameIdentifier: PsiIdentifier by lazyPub {
        LightIdentifier(parent.manager, constantValue.name.asString())
    }

    override fun getNameIdentifier(): PsiIdentifier = _nameIdentifier

    override fun getValue(): PsiAnnotationMemberValue? = _value

    override fun getLiteralValue(): String? = (value as? PsiLiteralExpression)?.value?.toString()

    override fun getName(): String = constantValue.name.asString()
}
