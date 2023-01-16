/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.buildClassType
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.isPrivateOrPrivateToThis
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolLightSimpleAnnotation
import org.jetbrains.kotlin.light.classes.symbol.isValid
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.nonExistentType
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtParameter

internal class SymbolLightSuspendContinuationParameter(
    private val functionSymbolPointer: KtSymbolPointer<KtFunctionSymbol>,
    private val containingMethod: SymbolLightMethodBase,
) : SymbolLightParameterBase(containingMethod) {
    private inline fun <T> withFunctionSymbol(crossinline action: context(KtAnalysisSession) (KtFunctionSymbol) -> T): T {
        return functionSymbolPointer.withSymbol(ktModule, action)
    }

    override fun getName(): String = SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME

    override fun getNameIdentifier(): PsiIdentifier? = null

    override fun getType(): PsiType = _type

    private val _type by lazyPub {
        withFunctionSymbol { functionSymbol ->
            buildClassType(StandardClassIds.Continuation) { argument(functionSymbol.returnType) }
                .asPsiType(this, allowErrorTypes = true)
                ?: nonExistentType()
        }
    }

    override fun isVarArgs(): Boolean = false

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightClassModifierList(
            containingDeclaration = this,
            staticModifiers = emptySet(),
        ) { modifierList ->
            if (withFunctionSymbol { it.visibility.isPrivateOrPrivateToThis() })
                emptyList()
            else
                listOf(SymbolLightSimpleAnnotation(NotNull::class.java.name, modifierList))
        }
    }

    override fun hasModifierProperty(p0: String): Boolean = false

    override val kotlinOrigin: KtParameter? = null

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightSuspendContinuationParameter &&
            containingMethod == other.containingMethod

    override fun hashCode(): Int = name.hashCode() * 31 + containingMethod.hashCode()

    override fun isValid(): Boolean = super.isValid() && functionSymbolPointer.isValid(ktModule)
}
