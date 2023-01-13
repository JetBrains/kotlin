/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class Fir2IrResult(
    val irModuleFragment: IrModuleFragment,
    val components: Fir2IrComponents,
    moduleDescriptor: FirModuleDescriptor
) {
    val pluginContext: Fir2IrPluginContext = Fir2IrPluginContext(components, moduleDescriptor)

    operator fun component1(): IrModuleFragment = irModuleFragment
    operator fun component2(): Fir2IrComponents = components
    operator fun component3(): Fir2IrPluginContext = pluginContext
}
