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
    override val IrDeclaration.mangleString: String
        get() = getMangleComputer(MangleMode.FULL).computeMangle(this)

    override val IrDeclaration.signatureString: String
        get() = getMangleComputer(MangleMode.SIGNATURE).computeMangle(this)

    override val IrDeclaration.fqnString: String
        get() = getMangleComputer(MangleMode.FQNAME).computeMangle(this)

    override fun IrDeclaration.isExported(): Boolean = getExportChecker().check(this, SpecialDeclarationType.REGULAR)
}