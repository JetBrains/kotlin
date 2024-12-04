/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrMangleComputer
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.NativeRuntimeNames
import org.jetbrains.kotlin.name.NativeStandardInteropNames

val ANNOTATIONS_TO_TREAT_AS_EXPORTED = listOf(
    NativeRuntimeNames.Annotations.symbolNameClassId,
    NativeRuntimeNames.Annotations.gcUnsafeCallClassId,
    NativeRuntimeNames.Annotations.exportForCppRuntimeClassId,
    NativeRuntimeNames.Annotations.cNameClassId,
    NativeRuntimeNames.Annotations.exportForCompilerClassId,
)

private val ANNOTATIONS_TO_TREAT_AS_EXPORTED_FQNS = ANNOTATIONS_TO_TREAT_AS_EXPORTED.map(ClassId::asSingleFqName)

abstract class AbstractKonanIrMangler(
        private val withReturnType: Boolean,
        private val allowOutOfScopeTypeParameters: Boolean = false
) : IrBasedKotlinManglerImpl() {
    override fun getExportChecker(compatibleMode: Boolean): IrExportCheckerVisitor = KonanIrExportChecker(compatibleMode)

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): IrMangleComputer =
            KonanIrManglerComputer(StringBuilder(256), mode, compatibleMode, withReturnType, allowOutOfScopeTypeParameters)

    override fun IrDeclaration.isPlatformSpecificExport(): Boolean {
        if (this is IrSimpleFunction) if (isFakeOverride) return false

        return ANNOTATIONS_TO_TREAT_AS_EXPORTED.any(this::hasAnnotation)
    }

    private inner class KonanIrExportChecker(compatibleMode: Boolean) : IrExportCheckerVisitor(compatibleMode) {
        override fun IrDeclaration.isPlatformSpecificExported(): Boolean = isPlatformSpecificExport()
    }

    private class KonanIrManglerComputer(
            builder: StringBuilder,
            mode: MangleMode,
            compatibleMode: Boolean,
            private val withReturnType: Boolean,
            allowOutOfScopeTypeParameters: Boolean,
    ) : IrMangleComputer(builder, mode, compatibleMode, allowOutOfScopeTypeParameters) {
        override fun copy(newMode: MangleMode): IrMangleComputer =
                KonanIrManglerComputer(builder, newMode, compatibleMode, withReturnType, allowOutOfScopeTypeParameters)

        override fun addReturnType(): Boolean = withReturnType

        override fun makePlatformSpecificFunctionNameMangleComputer(function: IrFunction) = IrObjCFunctionNameMangleComputer(function)

        override fun IrFunction.platformSpecificFunctionMarks(): List<String> = when (origin) {
            IrDeclarationOrigin.LOWERED_SUSPEND_FUNCTION -> listOfSuspendFunctionMark
            else -> emptyList()
        }

        companion object {
            val listOfSuspendFunctionMark = listOf(MangleConstant.SUSPEND_FUNCTION_MARK)
        }
    }
}

object KonanManglerIr : AbstractKonanIrMangler(false)

abstract class AbstractKonanDescriptorMangler : DescriptorBasedKotlinManglerImpl() {
    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<DeclarationDescriptor> = KonanDescriptorExportChecker()

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): DescriptorMangleComputer = KonanDescriptorMangleComputer(StringBuilder(256), mode)

    private inner class KonanDescriptorExportChecker : DescriptorExportCheckerVisitor() {
        override fun DeclarationDescriptor.isPlatformSpecificExported(): Boolean = isPlatformSpecificExport()
    }

    override fun DeclarationDescriptor.isPlatformSpecificExport(): Boolean {
        if (this is SimpleFunctionDescriptor) {
            if (kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) return false
        }

        return ANNOTATIONS_TO_TREAT_AS_EXPORTED_FQNS.any(annotations::hasAnnotation)
    }


    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private class KonanDescriptorMangleComputer(builder: StringBuilder, mode: MangleMode) : DescriptorMangleComputer(builder, mode) {
        override fun copy(newMode: MangleMode): DescriptorMangleComputer = KonanDescriptorMangleComputer(builder, newMode)

        override fun makePlatformSpecificFunctionNameMangleComputer(function: FunctionDescriptor) =
            DescriptorObjCFunctionNameMangleComputer(function)
    }
}

object KonanManglerDesc : AbstractKonanDescriptorMangler()
