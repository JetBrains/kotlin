/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

interface IrGenerationExtension {
    companion object :
        ProjectExtensionDescriptor<IrGenerationExtension>(
            "org.jetbrains.kotlin.rGenerationExtension", IrGenerationExtension::class.java
        )

    fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext)
}

// Extension point for plugins which run before any lowerings, but after the Ir has been constructed.
@Deprecated("This is a temporary class which will be replaced with another extension mechanism soon.", level = DeprecationLevel.ERROR)
interface PureIrGenerationExtension {
    @Suppress("DEPRECATION_ERROR")
    companion object :
        ProjectExtensionDescriptor<PureIrGenerationExtension>(
            "org.jetbrains.kotlin.pureIrGenerationExtension", PureIrGenerationExtension::class.java
        )

    fun generate(moduleFragment: IrModuleFragment, context: CommonBackendContext)
}
