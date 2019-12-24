/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.ir.DeclarationFactory
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.factories.IrDeclarationFactory
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.name.FqName

interface BackendContext {
    val ir: Ir<CommonBackendContext>
    val builtIns: KotlinBuiltIns
    val irBuiltIns: IrBuiltIns
    val irDeclarationFactory: IrDeclarationFactory
    val sharedVariablesManager: SharedVariablesManager
    val declarationFactory: DeclarationFactory
    val internalPackageFqn: FqName
    val transformedFunction: MutableMap<IrFunctionSymbol, IrSimpleFunctionSymbol>
    val lateinitNullableFields: MutableMap<IrField, IrField>
}
