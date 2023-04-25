/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.signaturer

import org.jetbrains.kotlin.backend.common.serialization.mangle.AbstractKotlinMangler
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrMetadataSourceOwner
import org.jetbrains.kotlin.ir.util.KotlinMangler

interface FirMangler : KotlinMangler<FirDeclaration> {
    override val manglerName: String
        get() = "Fir"
}

class Ir2FirManglerAdapter(private val delegate: FirMangler) : AbstractKotlinMangler<IrDeclaration>(),
    KotlinMangler.IrMangler {
    override val manglerName: String
        get() = delegate.manglerName

    private fun IrDeclaration.fir(): FirDeclaration = ((this as IrMetadataSourceOwner).metadata as FirMetadataSource).fir!!

    override fun IrDeclaration.isExported(compatibleMode: Boolean): Boolean = delegate.run { fir().isExported(compatibleMode) }

    override fun IrDeclaration.mangleString(compatibleMode: Boolean): String = delegate.run { fir().mangleString(compatibleMode) }

    override fun IrDeclaration.signatureString(compatibleMode: Boolean): String = delegate.run { fir().signatureString(compatibleMode) }

    override fun IrDeclaration.fqnString(compatibleMode: Boolean): String = delegate.run { fir().fqnString(compatibleMode) }

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<IrDeclaration> =
        error("Should not have been reached")

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<IrDeclaration> {
        error("Should not be called")
    }
}
