/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrMangleComputer
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.lazy.descriptors.isJavaField
import org.jetbrains.kotlin.load.java.typeEnhancement.hasEnhancedNullability
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext

object JvmIrMangler : IrBasedKotlinManglerImpl() {
    private class JvmIrExportChecker(compatibleMode: Boolean) : IrExportCheckerVisitor(compatibleMode) {
        override fun IrDeclaration.isPlatformSpecificExported() = false
    }

    private class JvmIrManglerComputer(builder: StringBuilder, mode: MangleMode, compatibleMode: Boolean) : IrMangleComputer(builder, mode, compatibleMode) {
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

class JvmDescriptorMangler(private val mainDetector: MainFunctionDetector?) : DescriptorBasedKotlinManglerImpl() {
    private object ExportChecker : DescriptorExportCheckerVisitor() {
        override fun DeclarationDescriptor.isPlatformSpecificExported() = true
    }

    private class JvmDescriptorManglerComputer(
        builder: StringBuilder,
        private val mainDetector: MainFunctionDetector?,
        mode: MangleMode
    ) : DescriptorMangleComputer(builder, mode) {
        override fun addReturnTypeSpecialCase(function: FunctionDescriptor): Boolean = true

        override fun copy(newMode: MangleMode): DescriptorMangleComputer = JvmDescriptorManglerComputer(builder, mainDetector, newMode)

        private fun isMainFunction(descriptor: FunctionDescriptor): Boolean =
            mainDetector != null && mainDetector.isMain(descriptor)

        override fun FunctionDescriptor.platformSpecificSuffix(): String? =
            if (isMainFunction(this)) source.containingFile.name else null

        override fun PropertyDescriptor.platformSpecificSuffix(): String? {
            // Since LV 1.4 there is a feature PreferJavaFieldOverload which allows to have java and kotlin
            // properties with the same signature on the same level.
            // For more details see JvmPlatformOverloadsSpecificityComparator.kt
            return if (isJavaField) MangleConstant.JAVA_FIELD_SUFFIX else null
        }

        override fun visitModuleDeclaration(descriptor: ModuleDescriptor) {
            // In general, having module descriptor as `containingDeclaration` for regular declaration is considered an error (in JS/Native)
            // because there should be `PackageFragmentDescriptor` in between
            // but on JVM there is `SyntheticJavaPropertyDescriptor` whose parent is a module. So let just skip it.
        }

        override fun mangleTypePlatformSpecific(type: KotlinType, tBuilder: StringBuilder) {
            // Disambiguate between 'double' and '@NotNull java.lang.Double' types in mixed Java/Kotlin class hierarchies
            if (SimpleClassicTypeSystemContext.hasEnhancedNullability(type)) {
                tBuilder.appendSignature(MangleConstant.ENHANCED_NULLABILITY_MARK)
            }
        }
    }

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<DeclarationDescriptor> = ExportChecker

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<DeclarationDescriptor> =
        JvmDescriptorManglerComputer(StringBuilder(256), mainDetector, mode)
}
