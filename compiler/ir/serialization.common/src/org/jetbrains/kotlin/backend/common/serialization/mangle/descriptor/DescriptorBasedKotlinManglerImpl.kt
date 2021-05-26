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
    private fun withMode(mode: MangleMode, descriptor: DeclarationDescriptor): String =
        getMangleComputer(mode).computeMangle(descriptor)

    override fun ClassDescriptor.mangleEnumEntryString(): String = withMode(MangleMode.FQNAME, this)

    override fun PropertyDescriptor.mangleFieldString(): String = mangleString()

    override fun DeclarationDescriptor.mangleString(): String = withMode(MangleMode.FULL, this)

    override fun DeclarationDescriptor.signatureString(): String = withMode(MangleMode.SIGNATURE, this)

    override fun DeclarationDescriptor.fqnString(): String = withMode(MangleMode.FQNAME, this)

    override fun DeclarationDescriptor.isExported(compatibleMode: Boolean): Boolean = getExportChecker(compatibleMode).check(this, SpecialDeclarationType.REGULAR)
}

class Ir2DescriptorManglerAdapter(private val delegate: DescriptorBasedKotlinManglerImpl) : AbstractKotlinMangler<IrDeclaration>(),
    KotlinMangler.IrMangler {
    override val manglerName: String
        get() = delegate.manglerName

    override fun IrDeclaration.isExported(compatibleMode: Boolean): Boolean {
        return delegate.run { descriptor.isExported(compatibleMode) }
    }

    override fun IrDeclaration.mangleString(): String {
        return when (this) {
            is IrEnumEntry -> delegate.run { descriptor.mangleEnumEntryString() }
            is IrField -> delegate.run { descriptor.mangleFieldString() }
            else -> delegate.run { descriptor.mangleString() }
        }
    }

    override fun IrDeclaration.signatureString(): String = delegate.run { descriptor.signatureString() }

    override fun IrDeclaration.fqnString(): String = delegate.run { descriptor.fqnString() }

    override fun getMangleComputer(mode: MangleMode): KotlinMangleComputer<IrDeclaration> =
        error("Should not have been reached")

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<IrDeclaration> {
        error("Should not be called")
    }
}