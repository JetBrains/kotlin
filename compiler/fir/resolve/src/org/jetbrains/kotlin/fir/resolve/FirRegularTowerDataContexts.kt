/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import java.util.*

enum class FirTowerDataMode {
    MEMBER_DECLARATION,
    NESTED_CLASS,
    COMPANION_OBJECT,
    CONSTRUCTOR_HEADER,
    ENUM_ENTRY,
    SPECIAL,
}

class FirRegularTowerDataContexts private constructor(
    private val modeMap: EnumMap<FirTowerDataMode, FirTowerDataContext>,
    val primaryConstructorPureParametersScope: FirLocalScope?,
    val primaryConstructorAllParametersScope: FirLocalScope?,
    val mode: FirTowerDataMode,
) {
    constructor(
        forMemberDeclarations: FirTowerDataContext,
        forNestedClasses: FirTowerDataContext? = null,
        forCompanionObject: FirTowerDataContext? = null,
        forConstructorHeaders: FirTowerDataContext? = null,
        forEnumEntries: FirTowerDataContext? = null,
        forSpecial: FirTowerDataContext? = null,
        primaryConstructorPureParametersScope: FirLocalScope? = null,
        primaryConstructorAllParametersScope: FirLocalScope? = null,
    ) : this(
        enumMap(forMemberDeclarations, forNestedClasses, forCompanionObject, forConstructorHeaders, forEnumEntries, forSpecial),
        primaryConstructorPureParametersScope,
        primaryConstructorAllParametersScope,
        FirTowerDataMode.MEMBER_DECLARATION
    )

    val currentContext: FirTowerDataContext?
        get() = modeMap[mode]

    fun copy(newContext: FirTowerDataContext): FirRegularTowerDataContexts {
        val modeMap = EnumMap<FirTowerDataMode, FirTowerDataContext>(FirTowerDataMode::class.java)
        modeMap.putAll(this.modeMap)
        modeMap[mode] = newContext
        return FirRegularTowerDataContexts(modeMap, primaryConstructorPureParametersScope, primaryConstructorAllParametersScope, mode)
    }

    fun copy(newMode: FirTowerDataMode): FirRegularTowerDataContexts {
        if (newMode == mode) return this
        return FirRegularTowerDataContexts(modeMap, primaryConstructorPureParametersScope, primaryConstructorAllParametersScope, newMode)
    }

    fun copyWithSpecial(newContext: FirTowerDataContext): FirRegularTowerDataContexts {
        val modeMap = EnumMap<FirTowerDataMode, FirTowerDataContext>(FirTowerDataMode::class.java)
        modeMap.putAll(this.modeMap)
        modeMap[FirTowerDataMode.SPECIAL] = newContext
        return FirRegularTowerDataContexts(
            modeMap, primaryConstructorPureParametersScope, primaryConstructorAllParametersScope, FirTowerDataMode.SPECIAL
        )
    }

    companion object {
        private fun enumMap(
            forMemberDeclarations: FirTowerDataContext,
            forNestedClasses: FirTowerDataContext?,
            forCompanionObject: FirTowerDataContext?,
            forConstructorHeaders: FirTowerDataContext?,
            forEnumEntries: FirTowerDataContext?,
            forSpecial: FirTowerDataContext?,
        ): EnumMap<FirTowerDataMode, FirTowerDataContext> {
            val modeMap = EnumMap<FirTowerDataMode, FirTowerDataContext>(FirTowerDataMode::class.java)
            modeMap[FirTowerDataMode.MEMBER_DECLARATION] = forMemberDeclarations
            modeMap[FirTowerDataMode.NESTED_CLASS] = forNestedClasses
            modeMap[FirTowerDataMode.COMPANION_OBJECT] = forCompanionObject
            modeMap[FirTowerDataMode.CONSTRUCTOR_HEADER] = forConstructorHeaders
            modeMap[FirTowerDataMode.ENUM_ENTRY] = forEnumEntries
            modeMap[FirTowerDataMode.SPECIAL] = forSpecial
            return modeMap
        }
    }
}