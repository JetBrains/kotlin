/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.lower.SingletonObjectJvmStaticTransformer
import org.jetbrains.kotlin.backend.jvm.serialization.deserializeFromByteArray
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement

class JvmIrDeserializerImpl : JvmIrDeserializer {
    override fun deserializeTopLevelClass(
        irClass: IrClass,
        irBuiltIns: IrBuiltIns,
        symbolTable: SymbolTable,
        irProviders: List<IrProvider>,
        extensions: JvmGeneratorExtensions,
    ): Boolean {
        val serializedIr = when (val source = irClass.source) {
            is KotlinJvmBinarySourceElement -> source.binaryClass.classHeader.serializedIr
            is JvmPackagePartSource -> source.knownJvmBinaryClass?.classHeader?.serializedIr
            else -> null
        } ?: return false
        deserializeFromByteArray(
            serializedIr, irBuiltIns, symbolTable, irProviders, irClass, JvmIrTypeSystemContext(irBuiltIns)
        )

        irClass.transform(SingletonObjectJvmStaticTransformer(irBuiltIns, extensions.cachedFields), null)

        return true
    }
}
