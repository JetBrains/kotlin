/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.metadata.BuiltinsSerializer
import org.jetbrains.kotlin.backend.jvm.metadata.MetadataSerializer
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.serialization.SerializableStringTable
import org.jetbrains.org.objectweb.asm.Type

interface JvmBackendExtension {
    fun createSerializer(
        context: JvmBackendContext, klass: IrClass, type: Type, classBuilder: ClassBuilder, parentSerializer: MetadataSerializer?,
    ): MetadataSerializer

    fun createModuleMetadataSerializer(context: JvmBackendContext): ModuleMetadataSerializer

    fun createBuiltinsSerializer(): BuiltinsSerializer
}

interface ModuleMetadataSerializer {
    fun serializeOptionalAnnotationClass(metadata: MetadataSource.Class, stringTable: SerializableStringTable): ProtoBuf.Class
}
