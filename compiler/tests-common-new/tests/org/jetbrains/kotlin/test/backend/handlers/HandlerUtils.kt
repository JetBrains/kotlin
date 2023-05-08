/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.TestModule

inline fun IrBackendInput.processAllIrModuleFragments(
    module: TestModule,
    processor: (irModuleFragment: IrModuleFragment, moduleName: String) -> Unit
) {
    dependentIrModuleFragments.forEach { processor(it, it.name.asString()) }
    processor(irModuleFragment, module.name)
}