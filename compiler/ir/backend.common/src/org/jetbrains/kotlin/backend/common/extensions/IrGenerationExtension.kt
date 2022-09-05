/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.linkage.IrDeserializer

interface IrGenerationExtension : IrDeserializer.IrLinkerExtension {
    companion object :
        ProjectExtensionDescriptor<IrGenerationExtension>(
            "org.jetbrains.kotlin.irGenerationExtension", IrGenerationExtension::class.java
        )

    fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext)

    fun getPlatformIntrinsicExtension(backendContext: BackendContext): IrIntrinsicExtension? = null
}

/**
 * This interface for common IR is empty because intrinsics are done in a platform-specific way (because of inliner).
 * Currently, only JVM intrinsics are supported via JvmIrIntrinsicExtension interface.
 */
interface IrIntrinsicExtension