/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.backend.Fir2IrSpecialSymbolProvider
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class Fir2IrJvmSpecialAnnotationSymbolProvider : Fir2IrSpecialSymbolProvider() {

    private val kotlinJvmInternalPackage by lazy {
        IrExternalPackageFragmentImpl(
            DescriptorlessExternalPackageFragmentSymbol(),
            ENHANCED_NULLABILITY_ID.packageFqName
        )
    }

    private val kotlinInternalIrPackage by lazy {
        IrExternalPackageFragmentImpl(
            DescriptorlessExternalPackageFragmentSymbol(),
            FLEXIBLE_NULLABILITY_ID.packageFqName
        )
    }

    override fun getClassSymbolById(id: ClassId): IrClassSymbol? =
        when (id) {
            ENHANCED_NULLABILITY_ID -> id.toIrClass(kotlinJvmInternalPackage).symbol
            FLEXIBLE_NULLABILITY_ID -> id.toIrClass(kotlinInternalIrPackage).symbol
            FLEXIBLE_MUTABILITY_ID -> id.toIrClass(kotlinInternalIrPackage).symbol
            RAW_TYPE_ANNOTATION_ID -> id.toIrClass(kotlinInternalIrPackage).symbol
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

    companion object {
        private val ENHANCED_NULLABILITY_ID = ClassId.topLevel(JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION)
        private val FLEXIBLE_NULLABILITY_ID =
            ClassId.topLevel(IrBuiltIns.KOTLIN_INTERNAL_IR_FQN.child(Name.identifier("FlexibleNullability")))
        private val FLEXIBLE_MUTABILITY_ID =
            ClassId.topLevel(IrBuiltIns.KOTLIN_INTERNAL_IR_FQN.child(Name.identifier("FlexibleMutability")))
        private val RAW_TYPE_ANNOTATION_ID =
            ClassId.topLevel(IrBuiltIns.KOTLIN_INTERNAL_IR_FQN.child(Name.identifier("RawType")))
    }
}
