/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.EnhancedNullability
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.FlexibleArrayElementVariance
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.FlexibleMutability
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.FlexibleNullability
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.RawTypeAnnotation
import kotlin.apply

class JvmIrSpecialAnnotationSymbolProvider(private val irFactory: IrFactory) {
    private val kotlinJvmInternalPackage by lazy {
        IrExternalPackageFragmentImpl(DescriptorlessExternalPackageFragmentSymbol(), JvmAnnotationNames.KOTLIN_JVM_INTERNAL)
    }

    private val kotlinInternalIrPackage by lazy {
        IrExternalPackageFragmentImpl(DescriptorlessExternalPackageFragmentSymbol(), IrBuiltIns.KOTLIN_INTERNAL_IR_FQN)
    }

    val enhancedNullabilityAnnotation: IrClassSymbol = EnhancedNullability.toIrClass(kotlinJvmInternalPackage).symbol
    val flexibleNullabilityAnnotation: IrClassSymbol = FlexibleNullability.toIrClass(kotlinInternalIrPackage).symbol
    val flexibleMutabilityAnnotation: IrClassSymbol = FlexibleMutability.toIrClass(kotlinInternalIrPackage).symbol
    val rawTypeAnnotation: IrClassSymbol = RawTypeAnnotation.toIrClass(kotlinInternalIrPackage).symbol
    val flexibleArrayElementVarianceAnnotation: IrClassSymbol = FlexibleArrayElementVariance.toIrClass(kotlinInternalIrPackage).symbol

    fun getClassSymbolById(id: ClassId): IrClassSymbol? =
        when (id) {
            EnhancedNullability -> enhancedNullabilityAnnotation
            FlexibleNullability -> flexibleNullabilityAnnotation
            FlexibleMutability -> flexibleMutabilityAnnotation
            RawTypeAnnotation -> rawTypeAnnotation
            FlexibleArrayElementVariance -> flexibleArrayElementVarianceAnnotation
            else -> null
        }

    private fun ClassId.toIrClass(parent: IrDeclarationParent): IrClass =
        irFactory.buildClass {
            kind = ClassKind.ANNOTATION_CLASS
            name = shortClassName
        }.apply {
            createImplicitParameterDeclarationWithWrappedDescriptor()
            this.parent = parent
            addConstructor {
                isPrimary = true
            }
        }
}
