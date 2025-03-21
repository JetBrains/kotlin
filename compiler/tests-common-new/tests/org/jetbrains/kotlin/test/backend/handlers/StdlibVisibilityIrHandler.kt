/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.util.CollectNotVisibleFromOriginalModuleIrVisitor
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class StdlibVisibilityIrHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    override fun processModule(module: TestModule, info: IrBackendInput) {
        val stdlibModule = info.irPluginContext.irBuiltIns.anyClass.owner.file.module
        val irFiles = info.irModuleFragment.files.sortedBy { it.name }
        val notVisibleElements = irFiles.flatMap { collectNotVisibleElements(it, stdlibModule) }
        assert(notVisibleElements.isEmpty()) { "Found not visible elements ${notVisibleElements.toSet().map { it.fqNameWhenAvailable }}" }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    private fun collectNotVisibleElements(element: IrElement, module: IrModuleFragment): List<IrSimpleFunction> =
        buildList<IrSimpleFunction> {
            element.accept(CollectNotVisibleFromOriginalModuleIrVisitor(this, module), null)
        }
}