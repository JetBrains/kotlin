/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.analyzer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.resolve.BindingContext

abstract class Issue {
    abstract fun execute(
        irModule: IrModuleFragment,
        moduleDescriptor: ModuleDescriptor,
        bindingContext: BindingContext
    )
}