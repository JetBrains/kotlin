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
import org.jetbrains.kotlin.analysis.api.lifetime.isValid
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolLightAnnotationForAnnotationCall
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeNullabilityAnnotation
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.nonExistentType
import org.jetbrains.kotlin.light.classes.symbol.nullabilityType
import org.jetbrains.kotlin.psi.KtParameter

context(KtAnalysisSession)
internal class SymbolLightParameterForReceiver private constructor(
    private val receiverType: KtType,
    private val context: KtSymbol,
    methodName: String,
    method: SymbolLightMethodBase
) : SymbolLightParameterBase(method) {

    companion object {
        context (KtAnalysisSession)
        fun tryGet(
            callableSymbol: KtCallableSymbol,
            method: SymbolLightMethodBase
        ): SymbolLightParameterForReceiver? {
            if (callableSymbol !is KtNamedSymbol) return null

            if (!callableSymbol.isExtension) return null
            val extensionTypeAndAnnotations = callableSymbol.receiverType ?: return null

            return SymbolLightParameterForReceiver(
                receiverType = extensionTypeAndAnnotations,
                context = callableSymbol,
                methodName = callableSymbol.name.asString(),
                method = method
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
        buildList {
            receiverType.nullabilityType.computeNullabilityAnnotation(this@SymbolLightParameterForReceiver)?.let { add(it) }
            receiverType.annotations.mapTo(this) {
                SymbolLightAnnotationForAnnotationCall(it, this@SymbolLightParameterForReceiver)
            }
        }
    }

    override fun getModifierList(): PsiModifierList = _modifierList
    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightClassModifierList(this, emptySet(), _annotations)
    }

    private val _type: PsiType by lazyPub {
        receiverType.asPsiType(this@SymbolLightParameterForReceiver) ?: nonExistentType()
    }

    override fun getType(): PsiType = _type

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is SymbolLightParameterForReceiver &&
                        receiverType == other.receiverType)

    override fun hashCode(): Int = kotlinOrigin.hashCode()

    override fun isValid(): Boolean = super.isValid() && context.isValid()
}


