/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.asPsiType
import org.jetbrains.kotlin.analysis.api.javaInterop.isPrimitiveBacked
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.hasFlexibleNullability
import org.jetbrains.kotlin.analysis.api.types.isMarkedNullable
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.NullabilityAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.annotations.suppressWildcard
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.psi.KtParameter

@OptIn(KaImplementationDetail::class)
internal class SymbolLightParameterForReceiver private constructor(
    override val symbolPointer: KaSymbolPointer<KaReceiverParameterSymbol>,
    methodName: String,
    method: SymbolLightMethodBase,
) : SymbolLightParameterBase(method), KaSymbolJavaView<KaReceiverParameterSymbol> {
    private inline fun <T> withReceiverSymbol(crossinline action: context(KaSession) (KaReceiverParameterSymbol) -> T): T =
        symbolPointer.withSymbol(useSiteModule, action)

    companion object {
        fun tryGet(
            callableSymbolPointer: KaSymbolPointer<KaCallableSymbol>,
            method: SymbolLightMethodBase
        ): SymbolLightParameterForReceiver? = callableSymbolPointer.withSymbol(method.useSiteModule) { callableSymbol ->
            if (callableSymbol !is KaNamedSymbol) return@withSymbol null
            if (!callableSymbol.isExtension) return@withSymbol null
            // Companion extensions hide their receiver from the JVM signature (KEEP-0449 §1.3.6, §4.1.3),
            // so the light class must not expose a leading receiver value parameter.
            @OptIn(KaExperimentalApi::class)
            if (callableSymbol.isCompanion) return@withSymbol null
            val receiverSymbol = callableSymbol.receiverParameter ?: return@withSymbol null

            SymbolLightParameterForReceiver(
                symbolPointer = receiverSymbol.createPointer(),
                methodName = callableSymbol.name.asString(),
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

    override fun getModifierList(): PsiModifierList = cachedValue {
        SymbolLightClassModifierList(
            containingDeclaration = this,
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(
                    useSiteModule = useSiteModule,
                    annotatedSymbolPointer = symbolPointer,
                ),
                additionalAnnotationsProvider = NullabilityAnnotationsProvider {
                    withReceiverSymbol { receiver ->
                        receiver.returnType.let {
                            if (it.isPrimitiveBacked || it.hasFlexibleNullability) NullabilityAnnotation.NOT_REQUIRED
                            else NullabilityAnnotation.create(it.isMarkedNullable)
                        }
                    }
                },
            ),
        )
    }

    private val _type: PsiType by lazyPub {
        withReceiverSymbol { receiver ->
            val ktType = receiver.returnType
            ktType.asPsiType(
                this@SymbolLightParameterForReceiver,
                allowErrorTypes = true,
                getTypeMappingMode(ktType),
                suppressWildcards = receiver.suppressWildcard() ?: method.suppressWildcards(),
                allowNonJvmPlatforms = true,
            )
        } ?: nonExistentType()
    }

    override fun getType(): PsiType = _type

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightParameterForReceiver &&
            useSiteModule == other.useSiteModule &&
            compareSymbolPointers(symbolPointer, other.symbolPointer)

    override fun hashCode(): Int = _name.hashCode()

    override fun isValid(): Boolean = super.isValid() && symbolPointer.isValid(useSiteModule)
}
