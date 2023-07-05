/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.js

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.backend.FirMangler
import org.jetbrains.kotlin.fir.backend.FirExportCheckerVisitor
import org.jetbrains.kotlin.fir.backend.FirMangleComputer
import org.jetbrains.kotlin.fir.declarations.FirDeclaration

@NoMutableState
class FirJsKotlinMangler : FirMangler() {

    private class JsFirExportChecker : FirExportCheckerVisitor() {
        override fun FirDeclaration.isPlatformSpecificExported() = false
    }

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<FirDeclaration> {
        return JsFirExportChecker()
    }

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<FirDeclaration> {
        return FirMangleComputer(StringBuilder(256), mode)
    }
}
