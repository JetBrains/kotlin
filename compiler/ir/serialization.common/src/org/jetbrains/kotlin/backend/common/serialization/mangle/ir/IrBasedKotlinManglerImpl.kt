/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle.ir

import org.jetbrains.kotlin.backend.common.serialization.mangle.AbstractKotlinMangler
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.SpecialDeclarationType
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.util.KotlinMangler

abstract class IrBasedKotlinManglerImpl : AbstractKotlinMangler<IrDeclaration>(), KotlinMangler.IrMangler {

    override fun IrDeclaration.mangleString(compatibleMode: Boolean): String = getMangleComputer(MangleMode.FULL, compatibleMode).computeMangle(this)

    override fun IrDeclaration.signatureString(compatibleMode: Boolean): String = getMangleComputer(MangleMode.SIGNATURE, compatibleMode).computeMangle(this)

    override fun IrDeclaration.isExported(compatibleMode: Boolean): Boolean =
        getExportChecker(compatibleMode).check(this, SpecialDeclarationType.REGULAR)
}