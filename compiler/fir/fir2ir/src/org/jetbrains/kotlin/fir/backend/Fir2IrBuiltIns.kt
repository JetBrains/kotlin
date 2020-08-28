/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.types.CompilerConeAttributes
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class Fir2IrBuiltIns(
    private val components: Fir2IrComponents,
    session: FirSession
) : Fir2IrComponents by components {
    private val extensionFunctionTypeAnnotationSymbol by lazy {
        session.firSymbolProvider.getClassLikeSymbolByFqName(CompilerConeAttributes.ExtensionFunctionType.ANNOTATION_CLASS_ID)!!
            .toSymbol(session, classifierStorage, ConversionTypeContext.DEFAULT) as IrClassSymbol
    }

    internal fun extensionFunctionTypeAnnotationConstructorCall() =
        IrConstructorCallImpl.fromSymbolOwner(
            extensionFunctionTypeAnnotationSymbol.defaultType,
            extensionFunctionTypeAnnotationSymbol.owner.declarations.firstIsInstance<IrConstructor>().symbol
        )
}