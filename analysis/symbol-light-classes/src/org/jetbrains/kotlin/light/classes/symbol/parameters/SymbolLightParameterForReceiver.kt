/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolLightAnnotationForAnnotationCall
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeNullabilityAnnotation
import org.jetbrains.kotlin.light.classes.symbol.classes.analyzeForLightClasses
import org.jetbrains.kotlin.light.classes.symbol.compareSymbolPointers
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.nonExistentType
import org.jetbrains.kotlin.light.classes.symbol.nullabilityType
import org.jetbrains.kotlin.light.classes.symbol.restoreSymbolOrThrowIfDisposed
import org.jetbrains.kotlin.psi.KtParameter

internal class SymbolLightParameterForReceiver private constructor(
    private val callableSymbolWithReceiverPointer: KtSymbolPointer<KtCallableSymbol>,
    methodName: String,
    method: SymbolLightMethodBase
) : SymbolLightParameterBase(method) {
    private inline fun <T> withReceiverType(crossinline action: context(KtAnalysisSession) (KtType) -> T): T {
        return analyzeForLightClasses(ktModule) {
            val callableSymbol = callableSymbolWithReceiverPointer.restoreSymbolOrThrowIfDisposed()
            action(this, requireNotNull(callableSymbol.receiverType))
        }
    }

    companion object {
        fun tryGet(
            callableSymbolPointer: KtSymbolPointer<KtCallableSymbol>,
            method: SymbolLightMethodBase
        ): SymbolLightParameterForReceiver? {
            val methodName = analyzeForLightClasses(method.ktModule) {
                val callableSymbol = callableSymbolPointer.restoreSymbolOrThrowIfDisposed()
                if (callableSymbol !is KtNamedSymbol) return@analyzeForLightClasses null
                if (!callableSymbol.isExtension || callableSymbol.receiverType == null) return@analyzeForLightClasses null
                callableSymbol.name.asString()
            } ?: return null

            return SymbolLightParameterForReceiver(
                callableSymbolWithReceiverPointer = callableSymbolPointer,
                methodName = methodName,
                method = method,
            )
        }
    }

    private val _name: String by lazyPub {
        AsmUtil.getLabeledThisName(methodName, AsmUtil.LABELED_THIS_PARAMETER, AsmUtil.RECEIVER_PARAMETER_NAME)
    }

    override fun getNameIdentifier(): PsiIdentifier? = null

    override fun getName(): String = _name

    override fun isVarArgs() = false
    override fun hasModifierProperty(name: String): Boolean = false

    override val kotlinOrigin: KtParameter? = null

    private val _annotations: List<PsiAnnotation> by lazyPub {
        withReceiverType { receiverType ->
            buildList {
                receiverType.nullabilityType.computeNullabilityAnnotation(this@SymbolLightParameterForReceiver)?.let { add(it) }
                receiverType.annotations.mapTo(this) {
                    SymbolLightAnnotationForAnnotationCall(it, this@SymbolLightParameterForReceiver)
                }
            }
        }
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _modifierList: PsiModifierList by lazy {
        SymbolLightClassModifierList(this, lazyOf(emptySet()), lazyOf(_annotations))
    }

    private val _type: PsiType by lazy {
        withReceiverType { receiverType ->
            receiverType.asPsiType(this@SymbolLightParameterForReceiver)
        } ?: nonExistentType()
    }

    override fun getType(): PsiType = _type

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightParameterForReceiver &&
            ktModule == other.ktModule &&
            compareSymbolPointers(ktModule, callableSymbolWithReceiverPointer, other.callableSymbolWithReceiverPointer)

    override fun hashCode(): Int = _name.hashCode()

    override fun isValid(): Boolean = super.isValid() && analyzeForLightClasses(ktModule) {
        callableSymbolWithReceiverPointer.restoreSymbol()?.receiverType != null
    }
}


