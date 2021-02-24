/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.resolve.BindingContext

interface TranslationPluginContext : IrGeneratorContext {

    val moduleDescriptor: ModuleDescriptor
    val bindingContext: BindingContext
    val symbolTable: ReferenceSymbolTable
    val typeTranslator: TypeTranslator
}