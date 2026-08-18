/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.impl.light.LightReferenceListBuilder
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeThrowsList
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasDeprecatedAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.suppressWildcardMode
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForInterfaceDefaultImpls
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterForDefaultImplsReceiver
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightSuspendContinuationParameter
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightValueParameter
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import java.util.*

@OptIn(KaImplementationDetail::class)
internal abstract class SymbolLightMethod<FType : KaFunctionSymbol> private constructor(
    override val symbolPointer: KaSymbolPointer<FType>,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
    protected val valueParameterPickMask: BitSet?,
    protected val functionDeclaration: KtCallableDeclaration?,
    override val kotlinOrigin: KtDeclaration?,
    isJvmExposedBoxed: Boolean,
) : SymbolLightMethodBase(
    lightMemberOrigin = lightMemberOrigin,
    containingClass = containingClass,
    methodIndex = methodIndex,
    isJvmExposedBoxed = isJvmExposedBoxed,
), KaSymbolJavaView<FType> {
    internal constructor(
        functionSymbol: FType,
        lightMemberOrigin: LightMemberOrigin?,
        containingClass: SymbolLightClassBase,
        methodIndex: Int,
        isJvmExposedBoxed: Boolean,
        valueParameterPickMask: BitSet? = null,
    ) : this(
        symbolPointer = kotlin.run {
            @Suppress("UNCHECKED_CAST")
            functionSymbol.createPointer() as KaSymbolPointer<FType>
        },
        lightMemberOrigin = lightMemberOrigin,
        containingClass = containingClass,
        methodIndex = methodIndex,
        valueParameterPickMask = valueParameterPickMask,
        functionDeclaration = functionSymbol.sourcePsiSafe(),
        kotlinOrigin = functionSymbol.sourcePsiSafe() ?: lightMemberOrigin?.originalElement ?: functionSymbol.psiSafe<KtDeclaration>(),
        isJvmExposedBoxed = isJvmExposedBoxed,
    )

    protected inline fun <T> withFunctionSymbol(crossinline action: context(KaSession) (FType) -> T): T =
        symbolPointer.withSymbol(useSiteModule, action)

    protected open fun createValueParameter(parameterSymbol: KaValueParameterSymbol, parameterIndex: Int): PsiParameter =
        SymbolLightValueParameter(parameterSymbol = parameterSymbol, containingMethod = this)

    private val _parametersList by lazyPub {
        SymbolLightParameterList(
            parent = this@SymbolLightMethod,
            correspondingCallablePointer = symbolPointer,
        ) { builder ->
            if (this@SymbolLightMethod.containingClass is SymbolLightClassForInterfaceDefaultImpls) {
                builder.addParameter(SymbolLightParameterForDefaultImplsReceiver(this@SymbolLightMethod))
            }

            withFunctionSymbol { functionSymbol ->
                functionSymbol.valueParameters.forEachIndexed { index, parameter ->
                    val needToSkip = valueParameterPickMask?.get(index) == false
                    if (!needToSkip) {
                        builder.addParameter(createValueParameter(parameter, index))
                    }
                }

                if ((functionSymbol as? KaNamedFunctionSymbol)?.isSuspend == true) {
                    builder.addParameter(
                        @Suppress("UNCHECKED_CAST")
                        SymbolLightSuspendContinuationParameter(
                            functionSymbolPointer = symbolPointer as KaSymbolPointer<KaNamedFunctionSymbol>,
                            containingMethod = this@SymbolLightMethod,
                        )
                    )
                }
            }
        }
    }

    private val _isDeprecated: Boolean by lazyPub {
        withFunctionSymbol { functionSymbol ->
            functionSymbol.hasDeprecatedAnnotation()
        }
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    override fun getNameIdentifier(): PsiIdentifier = KtLightIdentifier(this, functionDeclaration)

    override fun getParameterList(): PsiParameterList = _parametersList

    override fun computeThrowsList(builder: LightReferenceListBuilder) {
        withFunctionSymbol { functionSymbol ->
            computeThrowsList(
                functionSymbol,
                builder,
                this@SymbolLightMethod,
                containingClass,
            )
        }
    }

    override fun isValid(): Boolean = super.isValid() && functionDeclaration?.isValid ?: symbolPointer.isValid(useSiteModule)

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return super.isEquivalentTo(another) || isOriginEquivalentTo(another)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null ||
            other::class != this::class ||
            (other as SymbolLightMethod<*>).methodIndex != methodIndex ||
            other.isJvmExposedBoxed != isJvmExposedBoxed ||
            other.useSiteModule != useSiteModule ||
            other.valueParameterPickMask != valueParameterPickMask
        ) return false

        if (functionDeclaration != null || other.functionDeclaration != null) {
            return functionDeclaration == other.functionDeclaration
        }

        return containingClass == other.containingClass &&
                compareSymbolPointers(symbolPointer, other.symbolPointer)
    }

    override fun hashCode(): Int = kotlinOrigin.hashCode()

    override fun suppressWildcards(): Boolean? =
        withFunctionSymbol { functionSymbol ->
            suppressWildcardMode(functionSymbol)
        }
}
