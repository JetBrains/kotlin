/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.org.objectweb.asm.Type

abstract class KotlinTypeMapperBase {
    abstract val typeSystem: TypeSystemCommonBackendContext

    abstract fun mapType(
        type: IrType,
        mode: TypeMappingMode = TypeMappingMode.DEFAULT,
        sw: JvmSignatureWriter? = null,
        materialized: Boolean = true,
    ): Type
}
