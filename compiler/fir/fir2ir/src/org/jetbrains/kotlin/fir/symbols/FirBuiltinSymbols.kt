/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.backend.common.ir.BuiltinSymbolsBase
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.name.Name

class FirBuiltinSymbols(
    irBuiltIns: IrBuiltIns,
    builtIns: KotlinBuiltIns,
    symbolTable: ReferenceSymbolTable
) : BuiltinSymbolsBase(irBuiltIns, builtIns, symbolTable) {

    init {
        val builtInsPackage = builtInsPackage("kotlin")
        // Comparable is a base interface for many pre-generated built-in classes,
        // like String, so it's easier if it's generated together with them
        // Otherwise we have problems like "IrLazyClass inherits Fir2IrLazyClass",
        // which does not work properly yet
        for (name in listOf("Comparable")) {
            (builtInsPackage.getContributedClassifier(
                Name.identifier(name),
                NoLookupLocation.FROM_BACKEND
            ) as? ClassDescriptor)?.let { symbolTable.referenceClass(it) }
        }
    }
}