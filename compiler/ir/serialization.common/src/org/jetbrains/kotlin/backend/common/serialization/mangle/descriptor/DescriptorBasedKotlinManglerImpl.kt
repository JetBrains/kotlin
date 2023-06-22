/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor

import org.jetbrains.kotlin.backend.common.serialization.mangle.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.types.KotlinType

abstract class DescriptorBasedKotlinManglerImpl : AbstractKotlinMangler<DeclarationDescriptor>(), KotlinMangler.DescriptorMangler {
    private fun withMode(mode: MangleMode, compatibleMode: Boolean, descriptor: DeclarationDescriptor): String =
        getMangleComputer(mode, compatibleMode).computeMangle(descriptor)

    override fun ClassDescriptor.mangleEnumEntryString(compatibleMode: Boolean): String = withMode(MangleMode.FQNAME, compatibleMode, this)

    override fun PropertyDescriptor.mangleFieldString(compatibleMode: Boolean): String = mangleString(compatibleMode)

    override fun DeclarationDescriptor.mangleString(compatibleMode: Boolean): String = withMode(MangleMode.FULL, compatibleMode, this)

    override fun DeclarationDescriptor.signatureString(compatibleMode: Boolean): String = withMode(MangleMode.SIGNATURE, compatibleMode, this)

    override fun DeclarationDescriptor.isExported(compatibleMode: Boolean): Boolean = getExportChecker(compatibleMode).check(this, SpecialDeclarationType.REGULAR)
}

class Ir2DescriptorManglerAdapter(private val delegate: DescriptorBasedKotlinManglerImpl) : AbstractKotlinMangler<IrDeclaration>(),
    KotlinMangler.IrMangler {
    override val manglerName: String
        get() = delegate.manglerName

    override fun IrDeclaration.isExported(compatibleMode: Boolean): Boolean {
        return delegate.run { descriptor.isExported(compatibleMode) }
    }

    override fun IrDeclaration.mangleString(compatibleMode: Boolean): String {
        return when (this) {
            is IrEnumEntry -> delegate.run { descriptor.mangleEnumEntryString(compatibleMode) }
            is IrField -> delegate.run { descriptor.mangleFieldString(compatibleMode) }
            else -> delegate.run { descriptor.mangleString(compatibleMode) }
        }
    }

    override fun IrDeclaration.signatureString(compatibleMode: Boolean): String = delegate.run { descriptor.signatureString(compatibleMode) }

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<IrDeclaration> =
        error("Should not have been reached")

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<IrDeclaration> {
        error("Should not be called")
    }
}