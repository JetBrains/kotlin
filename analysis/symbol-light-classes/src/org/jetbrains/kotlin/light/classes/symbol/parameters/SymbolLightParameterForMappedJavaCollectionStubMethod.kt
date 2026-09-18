/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForMappedJavaCollectionStubMethod
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.utils.cachedValue
import org.jetbrains.kotlin.psi.KtParameter

/**
 * A parameter of [SymbolLightMethodForMappedJavaCollectionStubMethod].
 *
 * It mirrors [javaParameter] of the overridden Java method (name and varargs), but the stub method is its owner,
 * so the parent chain and the containing file lead to the Kotlin light class rather than to the Java declaration.
 * The type is computed lazily by the stub method, as it has to be substituted according to the Kotlin class.
 *
 * The parameter has no Kotlin symbol of its own, so, like the stub method itself,
 * it is a view on the symbol of the containing Kotlin class.
 */
internal class SymbolLightParameterForMappedJavaCollectionStubMethod(
    private val javaParameter: PsiParameter,
    private val index: Int,
    private val containingMethod: SymbolLightMethodForMappedJavaCollectionStubMethod,
) : SymbolLightParameterBase<KaNamedClassSymbol>(containingMethod) {
    override val symbolPointer: KaSymbolPointer<KaNamedClassSymbol>
        get() = containingMethod.symbolPointer

    override val kotlinOrigin: KtParameter?
        get() = null

    override fun getName(): String = javaParameter.name

    override fun getNameIdentifier(): PsiIdentifier = KtLightIdentifier(lightOwner = this, ktDeclaration = null, name = name)

    private val _type: PsiType by lazyPub { containingMethod.computeParameterType(javaParameter, index) }

    override fun getType(): PsiType = _type

    override fun isVarArgs(): Boolean = javaParameter.isVarArgs

    override fun getModifierList(): PsiModifierList = cachedValue {
        SymbolLightClassModifierList(containingDeclaration = this)
    }

    override fun hasModifierProperty(name: String): Boolean = false

    override fun getNavigationElement(): PsiElement = javaParameter.navigationElement

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightParameterForMappedJavaCollectionStubMethod &&
            javaParameter == other.javaParameter &&
            method == other.method

    override fun hashCode(): Int = javaParameter.hashCode() * 31 + method.hashCode()
}
