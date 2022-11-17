/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiParameterList
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.psiSafe
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasDeprecatedAnnotation
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.classes.analyzeForLightClasses
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameter
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightSuspendContinuationParameter
import org.jetbrains.kotlin.light.classes.symbol.restoreSymbolOrThrowIfDisposed
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import java.util.*

internal abstract class SymbolLightMethod<FType : KtFunctionLikeSymbol>(
    functionSymbol: FType,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
    private val argumentsSkipMask: BitSet? = null,
) : SymbolLightMethodBase(
    lightMemberOrigin,
    containingClass,
    methodIndex,
) {
    protected val functionDeclaration: KtCallableDeclaration? = functionSymbol.sourcePsiSafe()

    @Suppress("UNCHECKED_CAST")
    protected val functionSymbolPointer: KtSymbolPointer<FType> = functionSymbol.createPointer() as KtSymbolPointer<FType>

    protected fun <T> withFunctionSymbol(action: KtAnalysisSession.(FType) -> T): T = analyzeForLightClasses(ktModule) {
        action(functionSymbolPointer.restoreSymbolOrThrowIfDisposed())
    }

    private val _isVarArgs: Boolean by lazy {
        functionDeclaration?.valueParameters?.any { it.isVarArg } ?: withFunctionSymbol { functionSymbol ->
            functionSymbol.valueParameters.any { it.isVararg }
        }
    }

    override fun isVarArgs(): Boolean = _isVarArgs

    private val _parametersList by lazyPub {
        SymbolLightParameterList(
            ktModule = ktModule,
            parent = this@SymbolLightMethod,
            callableWithReceiverSymbolPointer = functionSymbolPointer,
        ) { builder ->
            withFunctionSymbol { functionSymbol ->
                functionSymbol.valueParameters.mapIndexed { index, parameter ->
                    val needToSkip = argumentsSkipMask?.get(index) == true
                    if (!needToSkip) {
                        builder.addParameter(
                            SymbolLightParameter(
                                parameterSymbol = parameter,
                                containingMethod = this@SymbolLightMethod,
                            )
                        )
                    }
                }

                if ((functionSymbol as? KtFunctionSymbol)?.isSuspend == true) {
                    builder.addParameter(
                        @Suppress("UNCHECKED_CAST")
                        SymbolLightSuspendContinuationParameter(
                            functionSymbolPointer = functionSymbolPointer as KtSymbolPointer<KtFunctionSymbol>,
                            containingMethod = this@SymbolLightMethod,
                        )
                    )
                }
            }
        }
    }

    private val _identifier: PsiIdentifier by lazy {
        KtLightIdentifier(this@SymbolLightMethod, functionDeclaration)
    }

    private val _isDeprecated: Boolean by lazyPub {
        withFunctionSymbol { functionSymbol ->
            functionSymbol.hasDeprecatedAnnotation()
        }
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    override fun getParameterList(): PsiParameterList = _parametersList

    override val kotlinOrigin: KtDeclaration? =
        functionDeclaration ?: lightMemberOrigin?.originalElement ?: functionSymbol.psiSafe<KtDeclaration>()

    override fun isValid(): Boolean = super.isValid() && functionDeclaration?.isValid ?: analyzeForLightClasses(ktModule) {
        functionSymbolPointer.restoreSymbol() != null
    }

    override fun isOverride(): Boolean = withFunctionSymbol { it.getDirectlyOverriddenSymbols().isNotEmpty() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightMethod<*>) return false

        if (functionDeclaration != null) {
            return functionDeclaration == other.functionDeclaration && fieldsEquals(other)
        }

        return other.functionDeclaration == null &&
                fieldsEquals(other) &&
                containingClass == other.containingClass &&
                analyzeForLightClasses(ktModule) {
                    functionSymbolPointer.restoreSymbol() == other.functionSymbolPointer.restoreSymbol()
                }
    }

    private fun fieldsEquals(other: SymbolLightMethod<*>): Boolean {
        return methodIndex == other.methodIndex &&
                ktModule == other.ktModule &&
                argumentsSkipMask == other.argumentsSkipMask
    }

    override fun hashCode(): Int = kotlinOrigin.hashCode()
}
