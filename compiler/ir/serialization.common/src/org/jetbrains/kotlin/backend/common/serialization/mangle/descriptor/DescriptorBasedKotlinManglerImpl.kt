/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor

import org.jetbrains.kotlin.backend.common.serialization.mangle.AbstractKotlinMangler
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.types.KotlinType

abstract class DescriptorBasedKotlinManglerImpl : AbstractKotlinMangler<DeclarationDescriptor>(), KotlinMangler.DescriptorMangler {
    private fun withMode(mode: MangleMode, descriptor: DeclarationDescriptor, localNameResolver: (DeclarationDescriptor) -> String?): String =
        getMangleComputer(mode, approximator).computeMangle(descriptor, localNameResolver)

    override fun ClassDescriptor.mangleEnumEntryString(): String = withMode(MangleMode.FQNAME, this) { null}

    override fun PropertyDescriptor.mangleFieldString(): String = mangleString()

    private var approximator: (KotlinType) -> KotlinType = { it }

    override fun setupTypeApproximation(app: (KotlinType) -> KotlinType) {
        approximator = app
    }

    override fun DeclarationDescriptor.mangleString(localNameResolver: (DeclarationDescriptor) -> String?): String = withMode(MangleMode.FULL, this, localNameResolver)

    override fun DeclarationDescriptor.signatureString(localNameResolver: (DeclarationDescriptor) -> String?): String = withMode(MangleMode.SIGNATURE, this, localNameResolver)

    override fun DeclarationDescriptor.fqnString(localNameResolver: (DeclarationDescriptor) -> String?): String = withMode(MangleMode.FQNAME, this, localNameResolver)

    override fun DeclarationDescriptor.isExported(compatibleMode: Boolean): Boolean = true
}

class Ir2DescriptorManglerAdapter(private val delegate: DescriptorBasedKotlinManglerImpl) : AbstractKotlinMangler<IrDeclaration>(),
    KotlinMangler.IrMangler {
    override val manglerName: String
        get() = delegate.manglerName

    override fun IrDeclaration.isExported(compatibleMode: Boolean): Boolean = true

    override fun IrDeclaration.mangleString(localNameResolver: (IrDeclaration) -> String?): String {
        return when (this) {
            is IrEnumEntry -> delegate.run { descriptor.mangleEnumEntryString() }
            is IrField -> delegate.run { descriptor.mangleFieldString() }
            else -> delegate.run { descriptor.mangleString() }
        }
    }

    override fun IrDeclaration.signatureString(localNameResolver: (IrDeclaration) -> String?): String
         = delegate.run { descriptor.signatureString() }

    override fun IrDeclaration.fqnString(localNameResolver: (IrDeclaration) -> String?): String
         = delegate.run { descriptor.fqnString() }

    override fun getMangleComputer(mode: MangleMode, app: (KotlinType) -> KotlinType): KotlinMangleComputer<IrDeclaration> =
        error("Should not have been reached")

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<IrDeclaration> {
        error("Should not be called")
    }
}