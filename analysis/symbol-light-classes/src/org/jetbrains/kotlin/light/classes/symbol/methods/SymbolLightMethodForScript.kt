/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.InitializedModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightScriptMainParameter
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind

internal sealed class SymbolLightMethodForScript(
    private val ktScript: KtScript,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
) : SymbolLightMethodBase(
    LightMemberOriginForDeclaration(ktScript, JvmDeclarationOriginKind.OTHER),
    containingClass,
    methodIndex
) {
    abstract override fun getName(): String

    override fun getNameIdentifier(): PsiIdentifier = KtLightIdentifier(this, ktDeclaration = null, name)

    override fun hasTypeParameters(): Boolean = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    override fun getParameterList(): PsiParameterList = _parameterList

    private val _parameterList by lazyPub {
        SymbolLightParameterList(
            parent = this@SymbolLightMethodForScript
        ) { builder ->
            builder.addParameter(
                SymbolLightScriptMainParameter("args", this@SymbolLightMethodForScript)
            )
        }
    }

    override fun equals(other: Any?): Boolean = other === this ||
            other is SymbolLightMethodForScript &&
            other.methodIndex == this.methodIndex &&
            other.ktScript == this.ktScript

    override fun hashCode(): Int = ktScript.hashCode().times(31).plus(methodIndex.hashCode())
}

internal class SymbolLightMethodForScriptDefaultConstructor(
    ktScript: KtScript,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
) : SymbolLightMethodForScript(
    ktScript,
    containingClass,
    methodIndex
) {
    override fun getName(): String = containingClass.name ?: ""

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightMemberModifierList(
            containingDeclaration = this@SymbolLightMethodForScriptDefaultConstructor,
            modifiersBox = InitializedModifiersBox(PsiModifier.PUBLIC)
        )
    }

    override fun getReturnType(): PsiType? = null

    override fun isConstructor(): Boolean = true
    override fun isOverride(): Boolean = false
    override fun isDeprecated(): Boolean = false
}

internal class SymbolLightMethodForScriptMain(
    ktScript: KtScript,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
) : SymbolLightMethodForScript(
    ktScript,
    containingClass,
    methodIndex
) {
    override fun getName(): String = "main"

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightMemberModifierList(
            containingDeclaration = this@SymbolLightMethodForScriptMain,
            modifiersBox = InitializedModifiersBox(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL)
        )
    }

    override fun getReturnType(): PsiType = PsiType.VOID

    override fun isConstructor(): Boolean = false
    override fun isOverride(): Boolean = false
    override fun isDeprecated(): Boolean = false
}