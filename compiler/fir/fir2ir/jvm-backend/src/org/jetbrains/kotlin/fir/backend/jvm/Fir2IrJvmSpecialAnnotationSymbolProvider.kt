/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.backend.Fir2IrSpecialSymbolProvider
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.ClassId

class Fir2IrJvmSpecialAnnotationSymbolProvider : Fir2IrSpecialSymbolProvider() {

    private val kotlinJvmInternalPackage by lazy {
        IrExternalPackageFragmentImpl(
            DescriptorlessExternalPackageFragmentSymbol(),
            ENHANCED_NULLABILITY_ID.packageFqName
        )
    }

    override fun getClassSymbolById(id: ClassId): IrClassSymbol? {
        if (id != ENHANCED_NULLABILITY_ID) return null
        return components.irFactory.buildClass {
            kind = ClassKind.ANNOTATION_CLASS
            name = ENHANCED_NULLABILITY_ID.shortClassName
        }.apply {
            createImplicitParameterDeclarationWithWrappedDescriptor()
            this.parent = kotlinJvmInternalPackage
            addConstructor {
                isPrimary = true
            }
        }.symbol
    }

    companion object {
        private val ENHANCED_NULLABILITY_ID = ClassId.topLevel(JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION)
    }
}