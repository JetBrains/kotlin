/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.types.CompilerConeAttributes
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class Fir2IrBuiltIns(private val components: Fir2IrComponents) : Fir2IrComponents by components {
    private val extensionFunctionTypeAnnotationSymbol by lazy {
        annotationSymbolById(CompilerConeAttributes.ExtensionFunctionType.ANNOTATION_CLASS_ID)
    }

    internal fun extensionFunctionTypeAnnotationConstructorCall() =
        extensionFunctionTypeAnnotationSymbol.toConstructorCall()

    private val kotlinJvmInternalPackage =
        IrExternalPackageFragmentImpl(
            DescriptorlessExternalPackageFragmentSymbol(),
            CompilerConeAttributes.EnhancedNullability.ANNOTATION_CLASS_ID.packageFqName
        )

    private val enhancedNullabilityAnnotationSymbol by lazy {
        irFactory.buildClass {
            kind = ClassKind.ANNOTATION_CLASS
            name = CompilerConeAttributes.EnhancedNullability.ANNOTATION_CLASS_ID.shortClassName
        }.apply {
            createImplicitParameterDeclarationWithWrappedDescriptor()
            this.parent = kotlinJvmInternalPackage
            addConstructor {
                isPrimary = true
            }
        }.symbol
    }

    internal fun enhancedNullabilityAnnotationConstructorCall() =
        enhancedNullabilityAnnotationSymbol.toConstructorCall()

    private fun annotationSymbolById(id: ClassId): IrClassSymbol =
        session.firSymbolProvider.getClassLikeSymbolByFqName(id)!!.toSymbol(
            session, classifierStorage, ConversionTypeContext.DEFAULT
        ) as IrClassSymbol

    private fun IrClassSymbol.toConstructorCall(): IrConstructorCallImpl =
        IrConstructorCallImpl.fromSymbolOwner(defaultType, owner.declarations.firstIsInstance<IrConstructor>().symbol)
}