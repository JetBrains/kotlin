/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.resolve.BindingContext

interface IrGenerationExtension {
    companion object :
        ProjectExtensionDescriptor<IrGenerationExtension>("org.jetbrains.kotlin.irGenerationExtension", IrGenerationExtension::class.java)

    fun generate(
        file: IrFile,
        backendContext: BackendContext,
        bindingContext: BindingContext
    )
}