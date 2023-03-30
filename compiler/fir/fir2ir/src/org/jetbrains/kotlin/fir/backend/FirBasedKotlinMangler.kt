/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.serialization.mangle.AbstractKotlinMangler
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.SpecialDeclarationType
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.signaturer.FirMangler

abstract class FirBasedKotlinMangler : AbstractKotlinMangler<FirDeclaration>(), FirMangler {
    override fun FirDeclaration.mangleString(compatibleMode: Boolean): String = getMangleComputer(MangleMode.FULL, compatibleMode).computeMangle(this)

    override fun FirDeclaration.signatureString(compatibleMode: Boolean): String = getMangleComputer(MangleMode.SIGNATURE, compatibleMode).computeMangle(this)

    override fun FirDeclaration.fqnString(compatibleMode: Boolean): String = getMangleComputer(MangleMode.FQNAME, compatibleMode).computeMangle(this)

    override fun FirDeclaration.isExported(compatibleMode: Boolean): Boolean = true

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<FirDeclaration> {
        return object : KotlinExportChecker<FirDeclaration> {
            override fun check(declaration: FirDeclaration, type: SpecialDeclarationType): Boolean = true

            override fun FirDeclaration.isPlatformSpecificExported(): Boolean = true
        }
    }
}