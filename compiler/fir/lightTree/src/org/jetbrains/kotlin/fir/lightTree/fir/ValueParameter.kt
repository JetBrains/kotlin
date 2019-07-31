/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.impl.FirMemberPropertyImpl
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirQualifiedAccessExpressionImpl
import org.jetbrains.kotlin.fir.lightTree.converter.ClassNameUtil
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.Modifier
import org.jetbrains.kotlin.fir.references.FirPropertyFromParameterCallableReference
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl

class ValueParameter(
    private val isVal: Boolean,
    private val isVar: Boolean,
    private val modifiers: Modifier,
    val firValueParameter: FirValueParameter,
    val destructuringDeclaration: DestructuringDeclaration? = null
) {
    fun hasValOrVar(): Boolean {
        return isVal || isVar
    }

    fun toFirProperty(): FirProperty {
        val name = this.firValueParameter.name
        var type = this.firValueParameter.returnTypeRef
        if (type is FirImplicitTypeRef) {
            type = FirErrorTypeRefImpl(this.firValueParameter.session, null, "Incomplete code")
        }

        return FirMemberPropertyImpl(
            this.firValueParameter.session,
            null,
            FirPropertySymbol(ClassNameUtil.callableIdForName(name)),
            name,
            modifiers.getVisibility(),
            modifiers.getModality(),
            modifiers.hasExpect(),
            modifiers.hasActual(),
            isOverride = modifiers.hasOverride(),
            isConst = false,
            isLateInit = false,
            receiverTypeRef = null,
            returnTypeRef = type,
            isVar = this.isVar,
            initializer = FirQualifiedAccessExpressionImpl(this.firValueParameter.session, null).apply {
                calleeReference = FirPropertyFromParameterCallableReference(
                    this@ValueParameter.firValueParameter.session, null, name, this@ValueParameter.firValueParameter.symbol
                )
            },
            getter = FirDefaultPropertyGetter(this.firValueParameter.session, null, type, modifiers.getVisibility()),
            setter = if (this.isVar) FirDefaultPropertySetter(
                this.firValueParameter.session,
                null,
                type,
                modifiers.getVisibility()
            ) else null,
            delegate = null
        ).apply { annotations += this@ValueParameter.firValueParameter.annotations }
    }
}