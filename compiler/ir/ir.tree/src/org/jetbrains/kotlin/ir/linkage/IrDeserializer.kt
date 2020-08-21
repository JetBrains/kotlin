/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.linkage

import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol

interface IrDeserializer : IrProvider {

    interface IrLinkerExtension {
        fun resolveSymbol(symbol: IrSymbol, context: TranslationPluginContext): IrDeclaration? = null
    }

    fun init(moduleFragment: IrModuleFragment?, extensions: Collection<IrLinkerExtension>) {}
    fun postProcess() {}
}
