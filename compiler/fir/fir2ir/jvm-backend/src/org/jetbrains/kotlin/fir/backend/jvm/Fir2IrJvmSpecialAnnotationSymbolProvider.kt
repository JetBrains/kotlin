/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.backend.Fir2IrSpecialSymbolProvider
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.EnhancedNullability
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.FlexibleArrayElementVariance
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.FlexibleMutability
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.FlexibleNullability
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.RawTypeAnnotation

class Fir2IrJvmSpecialAnnotationSymbolProvider : Fir2IrSpecialSymbolProvider() {

    private val kotlinJvmInternalPackage by lazy {
        IrExternalPackageFragmentImpl(
            DescriptorlessExternalPackageFragmentSymbol(),
            EnhancedNullability.packageFqName
        )
    }

    private val kotlinInternalIrPackage by lazy {
        IrExternalPackageFragmentImpl(
            DescriptorlessExternalPackageFragmentSymbol(),
            FlexibleNullability.packageFqName
        )
    }

    override fun getClassSymbolById(id: ClassId): IrClassSymbol? =
        when (id) {
            EnhancedNullability -> id.toIrClass(kotlinJvmInternalPackage).symbol
            FlexibleNullability -> id.toIrClass(kotlinInternalIrPackage).symbol
            FlexibleMutability -> id.toIrClass(kotlinInternalIrPackage).symbol
            RawTypeAnnotation -> id.toIrClass(kotlinInternalIrPackage).symbol
            FlexibleArrayElementVariance -> id.toIrClass(kotlinInternalIrPackage).symbol
            else -> null
        }

    private fun ClassId.toIrClass(parent: IrDeclarationParent): IrClass =
        components.irFactory.buildClass {
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
