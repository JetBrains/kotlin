/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrMangleComputer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.types.KotlinType

abstract class AbstractJsManglerIr : IrBasedKotlinManglerImpl() {

    companion object {
//        private val exportChecker = JsIrExportChecker()
    }

    private class JsIrExportChecker(compatibleMode: Boolean) : IrExportCheckerVisitor(compatibleMode) {
        override fun IrDeclaration.isPlatformSpecificExported() = false
    }

    private class JsIrManglerComputer(builder: StringBuilder, mode: MangleMode) : IrMangleComputer(builder, mode) {
        override fun copy(newMode: MangleMode): IrMangleComputer = JsIrManglerComputer(builder, newMode)
    }

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<IrDeclaration> = JsIrExportChecker(compatibleMode)

    override fun getMangleComputer(mode: MangleMode, app: (KotlinType) -> KotlinType): KotlinMangleComputer<IrDeclaration> {
        return JsIrManglerComputer(StringBuilder(256), mode)
    }
}

object JsManglerIr : AbstractJsManglerIr()

abstract class AbstractJsDescriptorMangler : DescriptorBasedKotlinManglerImpl() {

    companion object {
//        private val exportChecker = JsDescriptorExportChecker()
    }

//    private class JsDescriptorExportChecker : DescriptorExportCheckerVisitor() {
//        override fun DeclarationDescriptor.isPlatformSpecificExported() = false
//    }

    private class JsDescriptorManglerComputer(builder: StringBuilder, mode: MangleMode, app: (KotlinType) -> KotlinType) : DescriptorMangleComputer(builder, mode, app) {
        override fun copy(newMode: MangleMode): DescriptorMangleComputer = JsDescriptorManglerComputer(builder, newMode, typeApproximation)
    }

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<DeclarationDescriptor> = error("Should not be reached")

    override fun getMangleComputer(mode: MangleMode, app: (KotlinType) -> KotlinType): KotlinMangleComputer<DeclarationDescriptor> {
        return JsDescriptorManglerComputer(StringBuilder(256), mode, app)
    }
}


object JsManglerDesc : AbstractJsDescriptorMangler()
