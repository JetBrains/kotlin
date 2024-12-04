/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.InitializedModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterList

internal class SymbolLightRepeatableAnnotationContainerMethod(
    private val annotationClassQualifier: String?,
    containingClass: SymbolLightClassBase,
) : SymbolLightMethodBase(
    lightMemberOrigin = null,
    containingClass = containingClass,
    methodIndex = METHOD_INDEX_BASE,
) {
    override fun getNameIdentifier(): PsiIdentifier? = null
    override fun isDeprecated(): Boolean = false
    override fun getName(): String = StandardNames.DEFAULT_VALUE_PARAMETER.asString()
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY
    override fun hasTypeParameters(): Boolean = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun isOverride(): Boolean = false

    private val _modifierList by lazyPub {
        SymbolLightMemberModifierList(
            containingDeclaration = this,
            modifiersBox = InitializedModifiersBox(PsiModifier.PUBLIC, PsiModifier.ABSTRACT),
        )
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _parameterList by lazyPub {
        SymbolLightParameterList(this)
    }

    override fun getParameterList(): PsiParameterList = _parameterList

    override fun isConstructor(): Boolean = false

    private val _returnType by lazyPub {
        val qualifier = annotationClassQualifier ?: return@lazyPub null
        JavaPsiFacade.getElementFactory(project).createTypeByFQClassName(qualifier, resolveScope).createArrayType()
    }

    override fun getReturnType(): PsiType? = _returnType

    override fun equals(other: Any?): Boolean =
        this === other || other is SymbolLightRepeatableAnnotationContainerMethod && other.containingClass == containingClass

    override fun hashCode(): Int = containingClass.hashCode()
    override fun copy(): PsiElement = SymbolLightRepeatableAnnotationContainerMethod(annotationClassQualifier, containingClass)
}
