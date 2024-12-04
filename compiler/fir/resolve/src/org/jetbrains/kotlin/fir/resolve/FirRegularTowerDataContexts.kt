/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import java.util.*

enum class FirTowerDataMode {
    REGULAR,
    CLASS_HEADER_ANNOTATIONS,
    NESTED_CLASS,
    COMPANION_OBJECT,
    CONSTRUCTOR_HEADER,
    ENUM_ENTRY,
}

class FirRegularTowerDataContexts private constructor(
    private val modeMap: EnumMap<FirTowerDataMode, FirTowerDataContext>,
    val primaryConstructorPureParametersScope: FirLocalScope?,
    val primaryConstructorAllParametersScope: FirLocalScope?,
    val activeMode: FirTowerDataMode,
) {
    constructor(
        regular: FirTowerDataContext,
        forClassHeaderAnnotations: FirTowerDataContext? = null,
        forNestedClasses: FirTowerDataContext? = null,
        forCompanionObject: FirTowerDataContext? = null,
        forConstructorHeaders: FirTowerDataContext? = null,
        forEnumEntries: FirTowerDataContext? = null,
        primaryConstructorPureParametersScope: FirLocalScope? = null,
        primaryConstructorAllParametersScope: FirLocalScope? = null,
    ) : this(
        enumMap(regular, forClassHeaderAnnotations, forNestedClasses, forCompanionObject, forConstructorHeaders, forEnumEntries),
        primaryConstructorPureParametersScope,
        primaryConstructorAllParametersScope,
        FirTowerDataMode.REGULAR
    )

    val currentContext: FirTowerDataContext?
        get() = modeMap[activeMode]

    fun replaceCurrentlyActiveContext(newContext: FirTowerDataContext): FirRegularTowerDataContexts {
        val modeMap = EnumMap<FirTowerDataMode, FirTowerDataContext>(FirTowerDataMode::class.java)
        modeMap.putAll(this.modeMap)
        modeMap[activeMode] = newContext
        return FirRegularTowerDataContexts(modeMap, primaryConstructorPureParametersScope, primaryConstructorAllParametersScope, activeMode)
    }

    fun replaceTowerDataMode(newMode: FirTowerDataMode): FirRegularTowerDataContexts {
        if (newMode == activeMode) return this
        return FirRegularTowerDataContexts(modeMap, primaryConstructorPureParametersScope, primaryConstructorAllParametersScope, newMode)
    }

    // Effectively equal to replaceTowerDataMode(REGULAR) + replaceCurrentlyActiveContext(newContext)
    // But left just for sake of optimization
    fun replaceAndSetActiveRegularContext(newContext: FirTowerDataContext): FirRegularTowerDataContexts {
        val modeMap = EnumMap<FirTowerDataMode, FirTowerDataContext>(FirTowerDataMode::class.java)
        modeMap.putAll(this.modeMap)
        modeMap[FirTowerDataMode.REGULAR] = newContext
        return FirRegularTowerDataContexts(
            modeMap, primaryConstructorPureParametersScope, primaryConstructorAllParametersScope, FirTowerDataMode.REGULAR
        )
    }

    companion object {
        private fun enumMap(
            regular: FirTowerDataContext,
            forClassHeaderAnnotations: FirTowerDataContext?,
            forNestedClasses: FirTowerDataContext?,
            forCompanionObject: FirTowerDataContext?,
            forConstructorHeaders: FirTowerDataContext?,
            forEnumEntries: FirTowerDataContext?,
        ): EnumMap<FirTowerDataMode, FirTowerDataContext> {
            val modeMap = EnumMap<FirTowerDataMode, FirTowerDataContext>(FirTowerDataMode::class.java)
            modeMap[FirTowerDataMode.REGULAR] = regular
            modeMap[FirTowerDataMode.CLASS_HEADER_ANNOTATIONS] = forClassHeaderAnnotations
            modeMap[FirTowerDataMode.NESTED_CLASS] = forNestedClasses
            modeMap[FirTowerDataMode.COMPANION_OBJECT] = forCompanionObject
            modeMap[FirTowerDataMode.CONSTRUCTOR_HEADER] = forConstructorHeaders
            modeMap[FirTowerDataMode.ENUM_ENTRY] = forEnumEntries
            return modeMap
        }
    }
}
