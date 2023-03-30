/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.common.serialization.mangle.*
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.backend.FirBasedKotlinMangler
import org.jetbrains.kotlin.fir.declarations.FirDeclaration

@NoMutableState
class FirJvmKotlinMangler : FirBasedKotlinMangler() {

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<FirDeclaration> {
        return FirJvmMangleComputer(StringBuilder(256), mode)
    }
}
