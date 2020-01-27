/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

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

    companion object {
        private val exportChecker = JsIrExportChecker()
    }

    private class JsIrExportChecker : IrExportCheckerVisitor() {
        override fun IrDeclaration.isPlatformSpecificExported() = false
    }

    private class JsIrManglerComputer(builder: StringBuilder, mode: MangleMode) : IrMangleComputer(builder, mode) {
        override fun copy(newMode: MangleMode): IrMangleComputer = JsIrManglerComputer(builder, newMode)
    }

    override fun getExportChecker(): KotlinExportChecker<IrDeclaration> = exportChecker

    override fun getMangleComputer(mode: MangleMode): KotlinMangleComputer<IrDeclaration> {
        return JsIrManglerComputer(StringBuilder(256), mode)
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

    private class JsDescriptorManglerComputer(builder: StringBuilder, mode: MangleMode) :
        DescriptorMangleComputer(builder, mode) {
        override fun copy(newMode: MangleMode): DescriptorMangleComputer = JsDescriptorManglerComputer(builder, newMode)
    }

    override fun getExportChecker(): KotlinExportChecker<DeclarationDescriptor> = exportChecker

    override fun getMangleComputer(mode: MangleMode): KotlinMangleComputer<DeclarationDescriptor> {
        return JsDescriptorManglerComputer(StringBuilder(256), mode)
    }
}


object JsManglerDesc : AbstractJsDescriptorMangler()
