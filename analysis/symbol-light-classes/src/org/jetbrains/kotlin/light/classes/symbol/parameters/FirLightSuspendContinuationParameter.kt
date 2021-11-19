/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.analysis.api.components.buildClassType
import org.jetbrains.kotlin.analysis.api.isValid
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.isPrivateOrPrivateToThis
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtParameter

internal class FirLightSuspendContinuationParameter(
    private val functionSymbol: KtFunctionSymbol,
    private val containingMethod: FirLightMethod,
) : FirLightParameter(containingMethod) {
    override fun getName(): String = SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    private val _identifier: PsiIdentifier by lazyPub {
        FirLightIdentifier(this, functionSymbol)
    }

    override fun getType(): PsiType = _type

    private val _type by lazyPub {
        analyzeWithSymbolAsContext(functionSymbol) {
            buildClassType(StandardClassIds.Continuation) {
                argument(functionSymbol.returnType)
            }.asPsiType(this@FirLightSuspendContinuationParameter)
        } ?: nonExistentType()
    }

    override fun isVarArgs(): Boolean = false

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _modifierList: PsiModifierList by lazyPub {
        val annotations =
            if (functionSymbol.visibility.isPrivateOrPrivateToThis()) emptyList() else
                listOf(
                    FirLightSimpleAnnotation(NotNull::class.java.name, this@FirLightSuspendContinuationParameter)
                )
        FirLightClassModifierList(this, emptySet(), annotations)
    }

    override fun hasModifierProperty(p0: String): Boolean = false

    override val kotlinOrigin: KtParameter? = null

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightSuspendContinuationParameter && functionSymbol == other.functionSymbol)

    override fun hashCode(): Int = functionSymbol.hashCode() * 31 + containingMethod.hashCode()

    override fun isValid(): Boolean = super.isValid() && functionSymbol.isValid()
}
