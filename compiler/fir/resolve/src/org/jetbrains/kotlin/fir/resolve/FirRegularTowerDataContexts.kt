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

class FirRegularTowerDataContexts(
    forMemberDeclarations: FirTowerDataContext,
    forNestedClasses: FirTowerDataContext? = null,
    forCompanionObject: FirTowerDataContext? = null,
    forConstructorHeaders: FirTowerDataContext? = null,
    forEnumEntries: FirTowerDataContext? = null,
    val primaryConstructorPureParametersScope: FirLocalScope? = null,
    val primaryConstructorAllParametersScope: FirLocalScope? = null,
) {
    private val modeMap = EnumMap<FirTowerDataMode, FirTowerDataContext>(FirTowerDataMode::class.java)

    init {
        modeMap[FirTowerDataMode.MEMBER_DECLARATION] = forMemberDeclarations
        modeMap[FirTowerDataMode.NESTED_CLASS] = forNestedClasses
        modeMap[FirTowerDataMode.COMPANION_OBJECT] = forCompanionObject
        modeMap[FirTowerDataMode.CONSTRUCTOR_HEADER] = forConstructorHeaders
        modeMap[FirTowerDataMode.ENUM_ENTRY] = forEnumEntries
    }

    var mode: FirTowerDataMode = FirTowerDataMode.MEMBER_DECLARATION

    var currentContext: FirTowerDataContext?
        get() = modeMap[mode]
        set(value) {
            modeMap[mode] = value
        }
}