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
import org.jetbrains.kotlin.ir.objcinterop.*
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.NativeRuntimeNames
import org.jetbrains.kotlin.name.NativeStandardInteropNames

private val annotationsToTreatAsExported = listOf(
    NativeRuntimeNames.Annotations.symbolNameClassId,
    NativeRuntimeNames.Annotations.gcUnsafeCallClassId,
    NativeRuntimeNames.Annotations.exportForCppRuntimeClassId,
    NativeRuntimeNames.Annotations.cNameClassId,
    NativeRuntimeNames.Annotations.exportForCompilerClassId,
)

private val IrConstructor.isObjCConstructor: Boolean
    get() = hasAnnotation(NativeStandardInteropNames.objCConstructorClassId)

abstract class AbstractKonanIrMangler(
        private val withReturnType: Boolean,
        private val allowOutOfScopeTypeParameters: Boolean = false
) : IrBasedKotlinManglerImpl() {
    override fun getExportChecker(compatibleMode: Boolean): IrExportCheckerVisitor = KonanIrExportChecker(compatibleMode)

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): IrMangleComputer =
            KonanIrManglerComputer(StringBuilder(256), mode, compatibleMode, withReturnType, allowOutOfScopeTypeParameters)

    override fun IrDeclaration.isPlatformSpecificExport(): Boolean {
        if (this is IrSimpleFunction) if (isFakeOverride) return false

        return annotationsToTreatAsExported.any(this::hasAnnotation)
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

        override fun IrFunction.platformSpecificFunctionName(): String? {
            (if (this is IrConstructor && this.isObjCConstructor) this.getObjCInitMethod() else this)?.getObjCMethodInfo()
                    ?.let {
                        return buildString {
                            if (extensionReceiverParameter != null) {
                                append(extensionReceiverParameter!!.type.getClass()!!.name)
                                append(MangleConstant.FQN_SEPARATOR)
                            }

                            append(MangleConstant.OBJC_MARK)
                            append(it.selector)
                            if (this@platformSpecificFunctionName is IrConstructor && this@platformSpecificFunctionName.isObjCConstructor) {
                                append(MangleConstant.OBJC_CONSTRUCTOR_MARK)
                            }

                            if ((this@platformSpecificFunctionName as? IrSimpleFunction)?.correspondingPropertySymbol != null) {
                                append(MangleConstant.OBJC_PROPERTY_ACCESSOR_MARK)
                            }
                        }
                    }
            return null
        }

        override fun IrFunction.platformSpecificFunctionMarks(): List<String> = when (origin) {
            IrDeclarationOrigin.LOWERED_SUSPEND_FUNCTION -> listOfSuspendFunctionMark
            else -> emptyList()
        }

        override fun IrFunction.specialValueParamPrefix(param: IrValueParameter): String {
            // TODO: there are clashes originating from ObjectiveC interop.
            // kotlinx.cinterop.ObjCClassOf<T>.create(format: kotlin.String): T defined in platform.Foundation in file Foundation.kt
            // and
            // kotlinx.cinterop.ObjCClassOf<T>.create(string: kotlin.String): T defined in platform.Foundation in file Foundation.kt

            return if (this.hasObjCMethodAnnotation || this.hasObjCFactoryAnnotation || this.isObjCClassMethod()) "${param.name}:" else ""
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

        return annotationsToTreatAsExported.any { annotations.hasAnnotation(it.asSingleFqName()) }
    }


    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private class KonanDescriptorMangleComputer(builder: StringBuilder, mode: MangleMode) : DescriptorMangleComputer(builder, mode) {
        override fun copy(newMode: MangleMode): DescriptorMangleComputer = KonanDescriptorMangleComputer(builder, newMode)

        override fun FunctionDescriptor.platformSpecificFunctionName(): String? {
            (if (this is ConstructorDescriptor && this.isObjCConstructor) this.getObjCInitMethod() else this)?.getObjCMethodInfo()
                    ?.let {
                        return buildString {
                            if (extensionReceiverParameter != null) {
                                append(extensionReceiverParameter!!.type.constructor.declarationDescriptor!!.name)
                                append(MangleConstant.FQN_SEPARATOR)
                            }

                            append(MangleConstant.OBJC_MARK)
                            append(it.selector)
                            if (this@platformSpecificFunctionName is ConstructorDescriptor && this@platformSpecificFunctionName.isObjCConstructor) {
                                append(MangleConstant.OBJC_CONSTRUCTOR_MARK)
                            }

                            if (this@platformSpecificFunctionName is PropertyAccessorDescriptor) {
                                append(MangleConstant.OBJC_PROPERTY_ACCESSOR_MARK)
                            }
                        }
                    }
            return null
        }

        override fun FunctionDescriptor.specialValueParamPrefix(param: ValueParameterDescriptor): String {
            return if (this.hasObjCMethodAnnotation || this.hasObjCFactoryAnnotation || this.isObjCClassMethod()) "${param.name}:" else ""
        }
    }
}

object KonanManglerDesc : AbstractKonanDescriptorMangler()
