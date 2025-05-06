/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType

class JvmEvaluatorData(
    // This is populated by LocalDeclarationsLowering with the intermediate data allowing mapping from local function captures to parameters
    // and accurate transformation of calls to local functions from code fragments.
    val localDeclarationsData: JvmBackendContext.SharedLocalDeclarationsData,
    // IR for synthetic evaluator-generated method which returns evaluated expression value
    val evaluatorGeneratedFunction: IrFunction,
    // If the code fragment captures some reified type parameters, we will need the corresponding arguments for the proper codegen
    // Bytecode might not contain all the required data, so we extract it from the debugger API and store here
    val capturedTypeParametersMapping: Map<IrTypeParameterSymbol, IrType>
)