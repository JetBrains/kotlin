/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject
import org.jetbrains.kotlin.light.classes.symbol.classes.isTypeParameter
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightValueParameterWithCustomType

/**
 * A light method for Kotlin collection method overrides or delegates that are mapped to Java collection methods
 * with special signatures.
 *
 * #### Example
 *
 * ```
 * abstract class MyCollection<Elem> : Collection<Elem> {
 *     override fun containsAll(elements: Collection<Elem>): Boolean {
 *         ...
 *     }
 * }
 *
 * abstract class MyCollection2<Elem> : Collection<Elem> by emptyList()
 * ```
 *
 * For the override and delegate methods `containsAll`, this class generates a light method with the remapped signature
 * `Collection<Elem>` -> `Collection<?>` that is expected by Java code.
 */
internal class SymbolLightMethodForMappedKotlinCollectionMethod(
    functionSymbol: KaNamedFunctionSymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassForClassOrObject,
    private val javaMethod: PsiMethod,
    private val substitutor: PsiSubstitutor,
    val isFinal: Boolean,
) : SymbolLightSimpleMethod(
    functionSymbol = functionSymbol,
    lightMemberOrigin = lightMemberOrigin,
    containingClass = containingClass,
    methodIndex = METHOD_INDEX_BASE,
    isTopLevel = false,
    valueParameterPickMask = null,
    suppressStatic = false,
    isJvmExposedBoxed = false,
) {
    init {
        val javaParameters = javaMethod.parameterList.parameters
        require(functionSymbol.valueParameters.size == javaParameters.size) {
            "Mapped Kotlin collection method '${functionSymbol.name}' has ${functionSymbol.valueParameters.size} Kotlin parameters " +
                    "but ${javaMethod.name} has ${javaParameters.size} Java parameters"
        }
    }

    override fun createValueParameter(parameterSymbol: KaValueParameterSymbol, parameterIndex: Int): PsiParameter =
        SymbolLightValueParameterWithCustomType(
            parameterSymbol = parameterSymbol,
            containingMethod = this,
            customType = mappedParameterTypes[parameterIndex],
        )

    private val mappedParameterTypes: List<PsiType> by lazyPub {
        javaMethod.parameterList.parameters.map { parameter ->
            val typeFromJava = parameter.type
            val substitutedType = substituteType(typeFromJava)
            if (typeFromJava.isTypeParameter()) substitutedType.unboxedOrSelf() else substitutedType
        }
    }

    private fun PsiType.unboxedOrSelf(): PsiType =
        PsiPrimitiveType.getUnboxedType(this)?.annotate(TypeAnnotationProvider.EMPTY) ?: this

    private fun substituteType(psiType: PsiType): PsiType =
        substitutor.substitute(psiType) ?: psiType

    override fun getReturnType(): PsiType =
        javaMethod.returnType?.let { substituteType(it) } ?: PsiTypes.voidType()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightMethodForMappedKotlinCollectionMethod) return false
        if (!super.equals(other)) return false

        if (javaMethod != other.javaMethod) return false
        if (isFinal != other.isFinal) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + javaMethod.hashCode()
        result = 31 * result + isFinal.hashCode()
        return result
    }
}
