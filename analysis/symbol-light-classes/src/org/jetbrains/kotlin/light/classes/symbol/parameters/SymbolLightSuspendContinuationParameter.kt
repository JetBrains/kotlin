/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.light.classes.symbol.NullabilityAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.EmptyAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.NullabilityAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.isValid
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.nonExistentType
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtParameter

internal class SymbolLightSuspendContinuationParameter(
    private val functionSymbolPointer: KaSymbolPointer<KaNamedFunctionSymbol>,
    private val containingMethod: SymbolLightMethodBase,
) : SymbolLightParameterBase(containingMethod) {
    private inline fun <T> withFunctionSymbol(crossinline action: KaSession.(KaNamedFunctionSymbol) -> T): T {
        return functionSymbolPointer.withSymbol(ktModule, action)
    }

    override fun getName(): String = SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME

    override fun getNameIdentifier(): PsiIdentifier? = null

    override fun getType(): PsiType = _type

    private val _type by lazyPub {
        withFunctionSymbol { functionSymbol ->
            val ktType = buildClassType(StandardClassIds.Continuation) { argument(functionSymbol.returnType) }
            ktType.asPsiType(
                this@SymbolLightSuspendContinuationParameter,
                allowErrorTypes = true,
                getTypeMappingMode(ktType),
                allowNonJvmPlatforms = true,
            ) ?: nonExistentType()
        }
    }

    override fun isVarArgs(): Boolean = false

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightClassModifierList(
            containingDeclaration = this,
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = EmptyAnnotationsProvider,
                additionalAnnotationsProvider = NullabilityAnnotationsProvider {
                    if (withFunctionSymbol { it.visibility == KaSymbolVisibility.PRIVATE })
                        NullabilityAnnotation.NOT_REQUIRED
                    else
                        NullabilityAnnotation.NON_NULLABLE
                },
            ),
        )
    }

    override fun hasModifierProperty(p0: String): Boolean = false

    override val kotlinOrigin: KtParameter? = null

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightSuspendContinuationParameter &&
            containingMethod == other.containingMethod

    override fun hashCode(): Int = name.hashCode() * 31 + containingMethod.hashCode()

    override fun isValid(): Boolean = super.isValid() && functionSymbolPointer.isValid(ktModule)
}
