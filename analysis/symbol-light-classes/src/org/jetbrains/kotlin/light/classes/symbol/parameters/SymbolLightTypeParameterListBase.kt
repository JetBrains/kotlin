/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.light.classes.symbol.utils.basicIsEquivalentTo
import org.jetbrains.kotlin.light.classes.symbol.utils.toArrayIfNotEmptyOrDefault
import javax.swing.Icon

/**
 * A base class for type parameter lists of symbol light declarations.
 *
 * It builds the parent chain from the list to its [owner], so the type parameters are reachable
 * from the containing light class, and provides the trivial part of [PsiTypeParameterList].
 */
internal abstract class SymbolLightTypeParameterListBase<out T : PsiTypeParameterListOwner>(
    internal val owner: T,
) : LightElement(owner.manager, KotlinLanguage.INSTANCE), PsiTypeParameterList {
    private val _typeParameters: List<PsiTypeParameter> by lazyPub { computeTypeParameters() }

    /**
     * Computes the type parameters of the list in the declaration order. The result is cached by the base class.
     */
    protected abstract fun computeTypeParameters(): List<PsiTypeParameter>

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitTypeParameterList(this)
        } else {
            visitor.visitElement(this)
        }
    }

    override fun processDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement
    ): Boolean = typeParameters.all { processor.execute(it, state) }

    override fun getTypeParameters(): Array<PsiTypeParameter> =
        _typeParameters.toArrayIfNotEmptyOrDefault(PsiTypeParameter.EMPTY_ARRAY)

    override fun getTypeParameterIndex(typeParameter: PsiTypeParameter): Int = _typeParameters.indexOf(typeParameter)

    override fun toString(): String = this::class.simpleName.orEmpty()
    override fun getElementIcon(flags: Int): Icon? = null

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int

    override fun isEquivalentTo(another: PsiElement?): Boolean = basicIsEquivalentTo(this, another)

    override fun getParent(): PsiElement = owner
    override fun getContainingFile(): PsiFile = parent.containingFile
}
