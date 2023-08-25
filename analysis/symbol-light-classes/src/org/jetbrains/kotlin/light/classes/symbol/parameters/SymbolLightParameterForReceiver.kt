/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.*
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAnnotationsMethod
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.psi.KtParameter

internal class SymbolLightParameterForReceiver private constructor(
    private val receiverPointer: KtSymbolPointer<KtReceiverParameterSymbol>,
    methodName: String,
    method: SymbolLightMethodBase,
) : SymbolLightParameterBase(method) {
    private inline fun <T> withReceiverSymbol(crossinline action: context(KtAnalysisSession) (KtReceiverParameterSymbol) -> T): T =
        receiverPointer.withSymbol(ktModule, action)

    companion object {
        fun tryGet(
            callableSymbolPointer: KtSymbolPointer<KtCallableSymbol>,
            method: SymbolLightMethodBase
        ): SymbolLightParameterForReceiver? = callableSymbolPointer.withSymbol(method.ktModule) { callableSymbol ->
            if (callableSymbol !is KtNamedSymbol) return@withSymbol null
            if (!callableSymbol.isExtension) return@withSymbol null
            val receiverSymbol = callableSymbol.receiverParameter ?: return@withSymbol null

            SymbolLightParameterForReceiver(
                receiverPointer = receiverSymbol.createPointer(),
                methodName = callableSymbol.name.asString(),
                method = method,
            )
        }
    }

    private val _name: String by lazyPub {
        if (method is SymbolLightAnnotationsMethod) "p0" else AsmUtil.getLabeledThisName(methodName, AsmUtil.LABELED_THIS_PARAMETER, AsmUtil.RECEIVER_PARAMETER_NAME)
    }

    override fun getNameIdentifier(): PsiIdentifier? = null

    override fun getName(): String = _name

    override fun isVarArgs() = false
    override fun hasModifierProperty(name: String): Boolean = false

    override val kotlinOrigin: KtParameter? = null

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _modifierList: PsiModifierList by lazyPub {
        if (method is SymbolLightAnnotationsMethod)
            SymbolLightClassModifierList(containingDeclaration = this)
        else SymbolLightClassModifierList(
            containingDeclaration = this,
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(
                    ktModule = ktModule,
                    annotatedSymbolPointer = receiverPointer,
                    annotationUseSiteTargetFilter = AnnotationUseSiteTarget.RECEIVER.toOptionalFilter(),
                ),
                additionalAnnotationsProvider = NullabilityAnnotationsProvider {
                    withReceiverSymbol { receiver ->
                        receiver.type.let { if (it.isPrimitiveBacked) NullabilityType.Unknown else it.nullabilityType }
                    }
                },
            ),
        )
    }

    private val _type: PsiType by lazyPub {
        withReceiverSymbol { receiver ->
            val ktType = receiver.type
            val psiType = ktType.asPsiTypeElement(
                this,
                allowErrorTypes = true,
                ktType.typeMappingMode()
            )?.let {
                annotateByKtType(it.type, ktType, it, modifierList)
            }
            if (method is SymbolLightAnnotationsMethod) {
                val erased = TypeConversionUtil.erasure(psiType)
                val name = erased.canonicalText
                method.getPropertyTypeParameters()
                    .firstOrNull { it.name == name }
                    ?.superTypes
                    ?.firstOrNull()
                    ?.let { TypeConversionUtil.erasure(it) }
                    ?: erased
            } else psiType
        } ?: nonExistentType()
    }

    override fun getType(): PsiType = _type

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightParameterForReceiver &&
            ktModule == other.ktModule &&
            compareSymbolPointers(receiverPointer, other.receiverPointer)

    override fun hashCode(): Int = _name.hashCode()

    override fun isValid(): Boolean = super.isValid() && receiverPointer.isValid(ktModule)
}
