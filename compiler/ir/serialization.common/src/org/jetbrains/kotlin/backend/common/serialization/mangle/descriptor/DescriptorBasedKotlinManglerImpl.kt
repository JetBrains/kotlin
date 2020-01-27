/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor

import org.jetbrains.kotlin.backend.common.serialization.mangle.AbstractKotlinMangler
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.SpecialDeclarationType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.util.KotlinMangler

abstract class DescriptorBasedKotlinManglerImpl : AbstractKotlinMangler<DeclarationDescriptor>(), KotlinMangler.DescriptorMangler {
    private fun withPrefix(prefix: String, descriptor: DeclarationDescriptor): String =
        getMangleComputer(prefix).computeMangle(descriptor)

    override fun ClassDescriptor.mangleEnumEntryString(): String = withPrefix(MangleConstant.ENUM_ENTRY_PREFIX, this)

    override fun ClassDescriptor.isExportEnumEntry(): Boolean =
        getExportChecker().check(this, SpecialDeclarationType.ENUM_ENTRY)

    override fun PropertyDescriptor.isExportField(): Boolean = false

    override fun PropertyDescriptor.mangleFieldString(): String = error("Fields supposed to be non-exporting")

    override val DeclarationDescriptor.mangleString: String
        get() = withPrefix(MangleConstant.EMPTY_PREFIX, this)

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

    override fun getExportChecker() = error("Should not have been reached")

    override fun getMangleComputer(prefix: String): KotlinMangleComputer<IrDeclaration> = error("Should not have been reached")
}