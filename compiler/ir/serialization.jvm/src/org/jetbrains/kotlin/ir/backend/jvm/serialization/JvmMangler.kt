/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrMangleComputer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.load.java.JvmAnnotationNames

object JvmIrMangler : BaseJvmIrMangler()
open class BaseJvmIrMangler : IrBasedKotlinManglerImpl() {
    private class JvmIrExportChecker(compatibleMode: Boolean) : IrExportCheckerVisitor(compatibleMode) {
        override fun IrDeclaration.isPlatformSpecificExported() = false
    }

    open class JvmIrManglerComputer(builder: StringBuilder, mode: MangleMode, compatibleMode: Boolean, useEffectiveTypeVariances: Boolean = false) :
        IrMangleComputer(builder, mode, compatibleMode, useEffectiveTypeVariances = useEffectiveTypeVariances) {
        override fun copy(newMode: MangleMode): IrMangleComputer =
            JvmIrManglerComputer(builder, newMode, compatibleMode)

        override fun addReturnTypeSpecialCase(function: IrFunction): Boolean = true

        override fun mangleTypePlatformSpecific(type: IrType, tBuilder: StringBuilder) {
            if (type.hasAnnotation(JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION)) {
                tBuilder.append(MangleConstant.ENHANCED_NULLABILITY_MARK)
            }
        }
    }

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<IrDeclaration> = JvmIrExportChecker(compatibleMode)

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<IrDeclaration> =
        JvmIrManglerComputer(StringBuilder(256), mode, compatibleMode)
}
