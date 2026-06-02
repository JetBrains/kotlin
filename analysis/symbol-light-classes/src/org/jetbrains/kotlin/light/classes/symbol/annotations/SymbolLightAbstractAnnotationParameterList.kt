/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotationParameterList
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.psi.KtElement

internal sealed class SymbolLightAbstractAnnotationParameterList(
    parent: SymbolLightAbstractAnnotation,
) : KtLightElementBase(parent), PsiAnnotationParameterList {
    override val kotlinOrigin: KtElement? get() = (parent as SymbolLightAbstractAnnotation).kotlinOrigin?.valueArgumentList

    /**
     * Unlike [KtLightElementBase.getText], this implementation does not return an empty string when there is no source PSI
     * (e.g., for a type-use annotation coming from a binary library). Instead, it reconstructs the argument list from the
     * attributes, so that consumers relying on the text, such as [com.intellij.psi.PsiNameHelper.appendAnnotations]
     * (triggered by [com.intellij.psi.PsiType.getCanonicalText] with `annotated = true`), see the actual arguments.
     *
     * The format mirrors `com.intellij.psi.impl.compiled.ClsAnnotationParameterListImpl`: an empty list yields an empty
     * string, and the implicit `value` name is omitted for a single attribute.
     */
    override fun getText(): String {
        kotlinOrigin?.text?.let { return it }

        val attributes = attributes
        if (attributes.isEmpty()) return ""

        return attributes.joinToString(separator = ", ", prefix = "(", postfix = ")") { attribute ->
            val value = attribute.value?.text.orEmpty()
            val name = attribute.name
            if (name == null || attributes.size == 1 && name == "value") value else "$name = $value"
        }
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitAnnotationParameterList(this)
        } else {
            visitor.visitElement(this)
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun SymbolLightAbstractAnnotation.symbolLightAnnotationParameterList(): SymbolLightAbstractAnnotationParameterList {
    return SymbolLightEmptyAnnotationParameterList(this)
}

internal fun SymbolLightAbstractAnnotation.symbolLightAnnotationParameterList(
    arguments: List<AnnotationArgument>,
): SymbolLightAbstractAnnotationParameterList = if (arguments.isNotEmpty()) {
    SymbolLightLazyAnnotationParameterList(this, lazyOf(arguments))
} else {
    symbolLightAnnotationParameterList()
}

internal inline fun SymbolLightAbstractAnnotation.symbolLightAnnotationParameterList(
    crossinline argumentsComputer: () -> List<AnnotationArgument>,
): SymbolLightAbstractAnnotationParameterList = SymbolLightLazyAnnotationParameterList(this, lazyPub { argumentsComputer() })
