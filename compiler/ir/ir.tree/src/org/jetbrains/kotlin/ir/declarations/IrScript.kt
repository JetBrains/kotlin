/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol

//TODO: make IrScript as IrPackageFragment, because script is used as a file, not as a class
//NOTE: declarations and statements stored separately
abstract class IrScript :
    IrDeclarationBase(), IrSymbolDeclaration<IrScriptSymbol>, IrDeclarationWithName,
    IrDeclarationParent, IrStatementContainer {

    // NOTE: is the result of the FE conversion, because there script interpreted as a class and has receiver
    abstract var thisReceiver: IrValueParameter
}
