/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin

class IrPluginDeclarationOrigin(val pluginKey: FirPluginKey) : IrDeclarationOrigin {
    override fun toString(): String {
        return "GENERATED[${pluginKey}]"
    }
}
