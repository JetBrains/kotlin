/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor

import org.jetbrains.kotlin.backend.common.serialization.mangle.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.util.KotlinMangler

abstract class DescriptorBasedKotlinManglerImpl : AbstractKotlinMangler<DeclarationDescriptor>(), KotlinMangler.DescriptorMangler {
    private fun withMode(mode: MangleMode, descriptor: DeclarationDescriptor): String =
        getMangleComputer(mode).computeMangle(descriptor)

    override fun ClassDescriptor.mangleEnumEntryString(): String = withMode(MangleMode.FQNAME, this)

    override fun ClassDescriptor.isExportEnumEntry(): Boolean =
        getExportChecker().check(this, SpecialDeclarationType.ENUM_ENTRY)

    override fun PropertyDescriptor.isExportField(): Boolean = false

    override fun PropertyDescriptor.mangleFieldString(): String = error("Fields supposed to be non-exporting")

    override val DeclarationDescriptor.mangleString: String
        get() = withMode(MangleMode.FULL, this)

    override val DeclarationDescriptor.signatureString: String
        get() = withMode(MangleMode.SIGNATURE, this)

    override val DeclarationDescriptor.fqnString: String
        get() = withMode(MangleMode.FQNAME, this)

    override fun DeclarationDescriptor.isExported(): Boolean = getExportChecker().check(this, SpecialDeclarationType.REGULAR)
}

class Ir2DescriptorManglerAdapter(private val delegate: DescriptorBasedKotlinManglerImpl) : AbstractKotlinMangler<IrDeclaration>(),
    KotlinMangler.IrMangler {
    override val manglerName: String
        get() = delegate.manglerName

    override fun IrDeclaration.isExported(): Boolean {
        return when (this) {
            is IrAnonymousInitializer -> false
            is IrEnumEntry -> delegate.run { descriptor.isExportEnumEntry() }
            is IrField -> delegate.run { descriptor.isExportField() }
            else -> delegate.run { descriptor.isExported() }
        }
    }

    override val IrDeclaration.mangleString: String
        get() {
            return when (this) {
                is IrEnumEntry -> delegate.run { descriptor.mangleEnumEntryString() }
                is IrField -> delegate.run { descriptor.mangleFieldString() }
                else -> delegate.run { descriptor.mangleString }
            }
        }

    override val IrDeclaration.signatureString: String
        get() = delegate.run { descriptor.signatureString }

    override val IrDeclaration.fqnString: String
        get() = delegate.run { descriptor.fqnString }

    override fun getExportChecker() = error("Should not have been reached")

    override fun getMangleComputer(mode: MangleMode): KotlinMangleComputer<IrDeclaration> = error("Should not have been reached")
}