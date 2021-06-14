/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeAlias

abstract class DeclarationProvider {
    abstract fun getClassByClassId(classId: ClassId): KtClassOrObject?

    abstract fun getTypeAliasByClassId(classId: ClassId): KtTypeAlias?

    abstract fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty>

    abstract fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction>

    abstract fun getClassNamesInPackage(packageFqName: FqName): Set<Name>
}