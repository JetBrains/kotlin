/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
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
    private val receiverPointer: KaSymbolPointer<KaReceiverParameterSymbol>,
    methodName: String,
    method: SymbolLightMethodBase,
) : SymbolLightParameterBase(method) {
    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    private inline fun <T> withReceiverSymbol(crossinline action: context(KaSession) (KaReceiverParameterSymbol) -> T): T =
        receiverPointer.withSymbol(ktModule, action)

    companion object {
        fun tryGet(
            callableSymbolPointer: KaSymbolPointer<KaCallableSymbol>,
            method: SymbolLightMethodBase
        ): SymbolLightParameterForReceiver? = callableSymbolPointer.withSymbol(method.ktModule) { callableSymbol ->
            if (callableSymbol !is KaNamedSymbol) return@withSymbol null
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
                        receiver.returnType.let { if (it.isPrimitiveBacked) KaTypeNullability.UNKNOWN else it.nullability }
                    }
                },
            ),
        )
    }

    private val _type: PsiType by lazyPub {
        withReceiverSymbol { receiver ->
            val ktType = receiver.returnType
            val psiType = ktType.asPsiType(
                this,
                allowErrorTypes = true,
                getTypeMappingMode(ktType),
                suppressWildcards = receiver.suppressWildcard() ?: method.suppressWildcards(),
            )

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
