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
import org.jetbrains.kotlin.fir.lightTree.ClassNameUtil
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.MemberModifier
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.Modifier
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.PlatformModifier
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

class ValueParameter(
    private val isVal: Boolean,
    private val isVar: Boolean,
    private val modifier: Modifier,
    val firValueParameter: FirValueParameter
) {
    fun hasValOrVar(): Boolean {
        return isVal || isVar
    }

    fun toFirProperty(): FirProperty {
        val name = this.firValueParameter.name
        val type = this.firValueParameter.returnTypeRef

        return FirMemberPropertyImpl(
            this.firValueParameter.session,
            null,
            FirPropertySymbol(ClassNameUtil.callableIdForName(name)),
            name,
            modifier.visibilityModifier.toVisibility(),
            modifier.inheritanceModifier?.toModality(),
            modifier.platformModifier == PlatformModifier.EXPECT,
            modifier.platformModifier == PlatformModifier.ACTUAL,
            isOverride = modifier.memberModifier == MemberModifier.OVERRIDE,
            isConst = false,
            isLateInit = false,
            receiverTypeRef = null,
            returnTypeRef = type,
            isVar = this.isVar,
            initializer = null,
            getter = FirDefaultPropertyGetter(this.firValueParameter.session, null, type, modifier.visibilityModifier.toVisibility()),
            setter = FirDefaultPropertySetter(this.firValueParameter.session, null, type, modifier.visibilityModifier.toVisibility()),
            delegate = null
        ).apply { annotations += this@ValueParameter.firValueParameter.annotations }
    }
}