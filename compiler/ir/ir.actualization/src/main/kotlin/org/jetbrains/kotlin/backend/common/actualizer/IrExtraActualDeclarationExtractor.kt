/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

/**
 * Allows extracting extra actual top-level declarations which are not presented in source code.
 * For instance, it allows extracting actual top-level classes and functions from builtin symbol provider (KT-65841).
 */
abstract class IrExtraActualDeclarationExtractor {
    abstract fun extract(expectIrClass: IrClass): IrClassSymbol?

    /**
     * The method works only with top-level callables.
     */
    abstract fun extract(expectTopLevelCallables: List<IrDeclarationWithName>, expectCallableId: CallableId): List<IrSymbol>
}

/**
 * Allows contributing additional pairs of symbols to [IrExpectActualMap], so they would be remapped during
 * the symbol updating passes of IR actualizer
 */
abstract class IrActualizerMapContributor {
    abstract fun collectClassesMap(): ActualClassInfo
    abstract fun collectTopLevelCallablesMap(): Map<IrSymbol, IrSymbol>
    abstract fun actualizeClass(classId: ClassId): IrClassSymbol?

    class ActualClassInfo(
        val classMapping: Map<IrClassSymbol, IrClassSymbol>,
        val actualTypeAliases: Map<ClassId, IrTypeAliasSymbol>
    )
}
