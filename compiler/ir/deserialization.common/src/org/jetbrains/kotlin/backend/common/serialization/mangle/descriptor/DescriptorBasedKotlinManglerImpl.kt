/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor

import org.jetbrains.kotlin.backend.common.serialization.mangle.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.util.KotlinMangler

abstract class DescriptorBasedKotlinManglerImpl : AbstractKotlinMangler<DeclarationDescriptor>(), KotlinMangler.DescriptorMangler {
    private fun withMode(mode: MangleMode, compatibleMode: Boolean, descriptor: DeclarationDescriptor): String =
        getMangleComputer(mode, compatibleMode).computeMangle(descriptor)

    override fun DeclarationDescriptor.signatureString(compatibleMode: Boolean): String = withMode(MangleMode.SIGNATURE, compatibleMode, this)

    override fun DeclarationDescriptor.isExported(compatibleMode: Boolean): Boolean = getExportChecker(compatibleMode).check(this, SpecialDeclarationType.REGULAR)
}
