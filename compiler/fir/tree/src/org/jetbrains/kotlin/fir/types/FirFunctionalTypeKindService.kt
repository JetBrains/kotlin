/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.name.FqName

abstract class FirFunctionalTypeKindService : FirSessionComponent {
    abstract fun getKindByClassNamePrefix(packageFqName: FqName, className: String): ConeFunctionalTypeKind?

    /*
     * Should be used only in places where session is unavaliable by default (e.g. in default cone type render)
     */
    object Default : FirFunctionalTypeKindService() {
        override fun getKindByClassNamePrefix(packageFqName: FqName, className: String): ConeFunctionalTypeKind? {
            return when (FunctionClassKind.parseClassName(className, packageFqName)?.kind) {
                FunctionClassKind.Function -> ConeFunctionalTypeKind.Function
                FunctionClassKind.SuspendFunction -> ConeFunctionalTypeKind.SuspendFunction
                FunctionClassKind.KFunction -> ConeFunctionalTypeKind.KFunction
                FunctionClassKind.KSuspendFunction -> ConeFunctionalTypeKind.KSuspendFunction
                null -> null
            }
        }
    }
}

val FirSession.functionalTypeService: FirFunctionalTypeKindService by FirSession.sessionComponentAccessor()
