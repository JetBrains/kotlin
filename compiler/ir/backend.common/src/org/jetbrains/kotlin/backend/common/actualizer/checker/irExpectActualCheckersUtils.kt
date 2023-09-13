/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer.checker

import org.jetbrains.kotlin.backend.common.actualizer.ClassActualizationInfo
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.util.classIdOrFail

internal fun getTypealiasSymbolIfActualizedViaTypealias(
    expectDeclaration: IrDeclaration,
    classActualizationInfo: ClassActualizationInfo,
): IrTypeAliasSymbol? {
    val topLevelExpectClass = getContainingTopLevelClass(expectDeclaration) ?: return null
    val classId = topLevelExpectClass.classIdOrFail
    return classActualizationInfo.actualTypeAliases[classId]
}

internal fun getContainingTopLevelClass(expectDeclaration: IrDeclaration): IrClass? {
    val parentsWithSelf = sequenceOf(expectDeclaration) + expectDeclaration.parents
    return parentsWithSelf.filterIsInstance<IrClass>().lastOrNull()
}