/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.IrSpecialAnnotationsProvider
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.EnhancedNullability
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.FlexibleArrayElementVariance
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.FlexibleMutability
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.FlexibleNullability
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.RawTypeAnnotation
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import kotlin.apply

object JvmIrSpecialAnnotationSymbolProvider : IrSpecialAnnotationsProvider() {
    private val kotlinJvmInternalPackage: IrExternalPackageFragmentImpl =
        IrExternalPackageFragmentImpl(DescriptorlessExternalPackageFragmentSymbol(), JvmAnnotationNames.KOTLIN_JVM_INTERNAL)

    private val kotlinInternalIrPackage: IrExternalPackageFragmentImpl =
        IrExternalPackageFragmentImpl(DescriptorlessExternalPackageFragmentSymbol(), IrBuiltIns.KOTLIN_INTERNAL_IR_FQN)

    private val enhancedNullabilityAnnotationInfo: AnnotationInfo = EnhancedNullability.getAnnotationInfo(kotlinJvmInternalPackage)
    private val flexibleNullabilityAnnotationInfo: AnnotationInfo = FlexibleNullability.getAnnotationInfo(kotlinInternalIrPackage)
    private val flexibleMutabilityAnnotationInfo: AnnotationInfo = FlexibleMutability.getAnnotationInfo(kotlinInternalIrPackage)
    private val rawTypeAnnotationInfo: AnnotationInfo = RawTypeAnnotation.getAnnotationInfo(kotlinInternalIrPackage)
    private val flexibleArrayElementVarianceAnnotationInfo: AnnotationInfo = FlexibleArrayElementVariance.getAnnotationInfo(kotlinInternalIrPackage)

    override fun generateEnhancedNullabilityAnnotationCall(): IrConstructorCall = enhancedNullabilityAnnotationInfo.toConstructorCall()
    override fun generateFlexibleNullabilityAnnotationCall(): IrConstructorCall = flexibleNullabilityAnnotationInfo.toConstructorCall()
    override fun generateFlexibleMutabilityAnnotationCall(): IrConstructorCall = flexibleMutabilityAnnotationInfo.toConstructorCall()
    override fun generateRawTypeAnnotationCall(): IrConstructorCall = rawTypeAnnotationInfo.toConstructorCall()
    override fun generateFlexibleArrayElementVarianceAnnotationCall(): IrConstructorCall =
        flexibleArrayElementVarianceAnnotationInfo.toConstructorCall()

    private fun AnnotationInfo.toConstructorCall(): IrConstructorCallImpl {
        return IrConstructorCallImpl.fromSymbolOwner(defaultType, constructorSymbol)
    }

    private class AnnotationInfo(val defaultType: IrType, val constructorSymbol: IrConstructorSymbol)

    private fun ClassId.getAnnotationInfo(irPackage: IrExternalPackageFragmentImpl): AnnotationInfo {
        val irClassSymbol = IrFactoryImpl.buildClass {
            kind = ClassKind.ANNOTATION_CLASS
            name = shortClassName
        }.apply {
            createImplicitParameterDeclarationWithWrappedDescriptor()
            parent = irPackage
            addConstructor {
                isPrimary = true
            }
        }.symbol
        val constructorSymbol = irClassSymbol.owner.declarations.firstIsInstance<IrConstructor>().symbol
        return AnnotationInfo(irClassSymbol.defaultType, constructorSymbol)
    }
}
