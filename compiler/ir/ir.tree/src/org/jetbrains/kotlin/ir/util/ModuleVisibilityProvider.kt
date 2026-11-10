/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.moduleVisibilityProvider

interface ModuleVisibilityProvider {
    fun canSeeInternals(thisModule: IrModuleFragment, otherModule: IrModuleFragment): Boolean
}

fun IrModuleFragment.canSeeInternalsOf(otherModule: IrModuleFragment): Boolean {
    return otherModule.moduleVisibilityProvider?.canSeeInternals(this, otherModule) == true
}
