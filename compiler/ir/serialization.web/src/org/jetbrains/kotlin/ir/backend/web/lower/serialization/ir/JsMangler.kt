/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.web.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrMangleComputer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration

abstract class AbstractJsManglerIr : IrBasedKotlinManglerImpl() {

    private class JsIrExportChecker(compatibleMode: Boolean) : IrExportCheckerVisitor(compatibleMode) {
        override fun IrDeclaration.isPlatformSpecificExported() = false
    }

    private class JsIrManglerComputer(builder: StringBuilder, mode: MangleMode, compatibleMode: Boolean) : IrMangleComputer(builder, mode, compatibleMode) {
        override fun copy(newMode: MangleMode): IrMangleComputer = JsIrManglerComputer(builder, newMode, compatibleMode)
    }

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<IrDeclaration> = JsIrExportChecker(compatibleMode)

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<IrDeclaration> {
        return JsIrManglerComputer(StringBuilder(256), mode, compatibleMode)
    }
}

object JsManglerIr : AbstractJsManglerIr()

abstract class AbstractJsDescriptorMangler : DescriptorBasedKotlinManglerImpl() {

    companion object {
        private val exportChecker = JsDescriptorExportChecker()
    }

    private class JsDescriptorExportChecker : DescriptorExportCheckerVisitor() {
        override fun DeclarationDescriptor.isPlatformSpecificExported() = false
    }

    private class JsDescriptorManglerComputer(builder: StringBuilder, mode: MangleMode) : DescriptorMangleComputer(builder, mode) {
        override fun copy(newMode: MangleMode): DescriptorMangleComputer = JsDescriptorManglerComputer(builder, newMode)
    }

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<DeclarationDescriptor> = exportChecker

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<DeclarationDescriptor> {
        return JsDescriptorManglerComputer(StringBuilder(256), mode)
    }
}


object JsManglerDesc : AbstractJsDescriptorMangler()
