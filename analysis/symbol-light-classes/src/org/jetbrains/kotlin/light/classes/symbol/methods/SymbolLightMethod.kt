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
import org.jetbrains.kotlin.light.classes.symbol.compareSymbolPointers
import org.jetbrains.kotlin.light.classes.symbol.isValid
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameter
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightSuspendContinuationParameter
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import java.util.*

internal abstract class SymbolLightMethod<FType : KtFunctionLikeSymbol> private constructor(
    protected val functionSymbolPointer: KtSymbolPointer<FType>,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
    private val argumentsSkipMask: BitSet?,
    protected val functionDeclaration: KtCallableDeclaration?,
    override val kotlinOrigin: KtDeclaration?,
) : SymbolLightMethodBase(
    lightMemberOrigin,
    containingClass,
    methodIndex,
) {
    internal constructor(
        ktAnalysisSession: KtAnalysisSession,
        functionSymbol: FType,
        lightMemberOrigin: LightMemberOrigin?,
        containingClass: SymbolLightClassBase,
        methodIndex: Int,
        argumentsSkipMask: BitSet? = null,
    ) : this(
        functionSymbolPointer = with(ktAnalysisSession) {
            @Suppress("UNCHECKED_CAST")
            functionSymbol.createPointer() as KtSymbolPointer<FType>
        },
        lightMemberOrigin = lightMemberOrigin,
        containingClass = containingClass,
        methodIndex = methodIndex,
        argumentsSkipMask = argumentsSkipMask,
        functionDeclaration = functionSymbol.sourcePsiSafe(),
        kotlinOrigin = functionSymbol.sourcePsiSafe() ?: lightMemberOrigin?.originalElement ?: functionSymbol.psiSafe<KtDeclaration>(),
    )

    protected fun <T> withFunctionSymbol(action: KtAnalysisSession.(FType) -> T): T = functionSymbolPointer.withSymbol(ktModule, action)

    private val _isVarArgs: Boolean by lazy {
        functionDeclaration?.valueParameters?.any { it.isVarArg } ?: withFunctionSymbol { functionSymbol ->
            functionSymbol.valueParameters.any { it.isVararg }
        }
    }

    override fun isVarArgs(): Boolean = _isVarArgs

    private val _parametersList by lazyPub {
        SymbolLightParameterList(
            parent = this@SymbolLightMethod,
            callableWithReceiverSymbolPointer = functionSymbolPointer,
        ) { builder ->
            withFunctionSymbol { functionSymbol ->
                functionSymbol.valueParameters.mapIndexed { index, parameter ->
                    val needToSkip = argumentsSkipMask?.get(index) == true
                    if (!needToSkip) {
                        builder.addParameter(
                            SymbolLightParameter(
                                ktAnalysisSession = this,
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

    private val _isDeprecated: Boolean by lazy {
        withFunctionSymbol { functionSymbol ->
            functionSymbol.hasDeprecatedAnnotation()
        }
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    override fun getParameterList(): PsiParameterList = _parametersList

    override fun isValid(): Boolean = super.isValid() && functionDeclaration?.isValid ?: functionSymbolPointer.isValid(ktModule)

    override fun isOverride(): Boolean = withFunctionSymbol { it.getDirectlyOverriddenSymbols().isNotEmpty() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightMethod<*> ||
            other.methodIndex != methodIndex ||
            other.ktModule != ktModule ||
            other.argumentsSkipMask != argumentsSkipMask
        ) return false

        if (functionDeclaration != null) {
            return functionDeclaration == other.functionDeclaration
        }

        return other.functionDeclaration == null &&
                containingClass == other.containingClass &&
                compareSymbolPointers(ktModule, functionSymbolPointer, other.functionSymbolPointer)
    }

    override fun hashCode(): Int = kotlinOrigin.hashCode()
}
