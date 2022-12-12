/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.klibdump

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.SpecialDeclarationType
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorMangleComputer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

// FIXME: Replace with JsManglerDesc after it is commonized.
object KlibDumpDescriptorMangler : DescriptorBasedKotlinManglerImpl() {

    private val exportChecker = JsDescriptorExportChecker()

    private class JsDescriptorExportChecker : DescriptorExportCheckerVisitor() {
        override fun DeclarationDescriptor.isPlatformSpecificExported() = false
    }

    private class JsDescriptorManglerComputer(builder: StringBuilder, mode: MangleMode) : DescriptorMangleComputer(builder, mode) {
        override fun copy(newMode: MangleMode): DescriptorMangleComputer = JsDescriptorManglerComputer(builder, newMode)
    }

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<DeclarationDescriptor> = exportChecker

    private class MangleComputer(builder: StringBuilder, mode: MangleMode) : DescriptorMangleComputer(builder, mode) {
        override fun copy(newMode: MangleMode): DescriptorMangleComputer = MangleComputer(builder, newMode)
    }

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<DeclarationDescriptor> {
        return MangleComputer(StringBuilder(256), mode)
    }
}
