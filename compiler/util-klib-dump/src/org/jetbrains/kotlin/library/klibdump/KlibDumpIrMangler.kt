/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.klibdump

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrBasedKotlinManglerImpl
import org.jetbrains.kotlin.ir.declarations.IrDeclaration

object KlibDumpIrMangler : IrBasedKotlinManglerImpl() {
    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<IrDeclaration> {
        TODO("Not yet implemented")
    }

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<IrDeclaration> {
        TODO("Not yet implemented")
    }
}
