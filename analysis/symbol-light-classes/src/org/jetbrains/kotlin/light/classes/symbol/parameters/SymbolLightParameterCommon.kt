/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.NullabilityType
import org.jetbrains.kotlin.light.classes.symbol.SymbolLightIdentifier
import org.jetbrains.kotlin.light.classes.symbol.getTypeNullability
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.nonExistentType
import org.jetbrains.kotlin.psi.KtParameter

context(KtAnalysisSession)
internal abstract class SymbolLightParameterCommon(
    private val parameterSymbol: KtValueParameterSymbol,
    private val containingMethod: SymbolLightMethodBase
) : SymbolLightParameterBase(containingMethod) {
    private val _name: String = parameterSymbol.name.asString()
    override fun getName(): String = _name

    override fun hasModifierProperty(name: String): Boolean =
        modifierList.hasModifierProperty(name)

    override val kotlinOrigin: KtParameter? = parameterSymbol.psi as? KtParameter

    abstract override fun getModifierList(): PsiModifierList

    private val _identifier: PsiIdentifier by lazyPub {
        SymbolLightIdentifier(this, parameterSymbol)
    }

    protected val nullabilityType: NullabilityType
        get() {
            val nullabilityApplicable = !containingMethod.containingClass.let { it.isAnnotationType || it.isEnum } &&
                    !containingMethod.hasModifierProperty(PsiModifier.PRIVATE)

            return if (nullabilityApplicable) getTypeNullability(parameterSymbol.returnType)
            else NullabilityType.Unknown
        }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    private val _type by lazyPub {
        val convertedType = run {
            val ktType = parameterSymbol.returnType
            val typeMappingMode = when {
                ktType.isSuspendFunctionType -> KtTypeMappingMode.DEFAULT
                // TODO: extract type mapping mode from annotation?
                // TODO: methods with declaration site wildcards?
                else -> KtTypeMappingMode.VALUE_PARAMETER
            }
            ktType.asPsiType(this@SymbolLightParameterCommon, typeMappingMode)
        } ?: nonExistentType()
        if (parameterSymbol.isVararg) {
            PsiEllipsisType(convertedType, convertedType.annotationProvider)
        } else convertedType
    }

    override fun getType(): PsiType = _type

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int
}
