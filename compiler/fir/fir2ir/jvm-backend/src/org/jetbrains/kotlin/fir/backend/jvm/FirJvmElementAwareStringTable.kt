/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.jvm.mapping.IrTypeMapper
import org.jetbrains.kotlin.backend.jvm.mapping.mapClass
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.serialization.FirElementAwareStringTable
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class FirJvmElementAwareStringTable(
    private val typeMapper: IrTypeMapper,
    private val components: Fir2IrComponents,
    nameResolver: JvmNameResolver? = null
) : JvmStringTable(nameResolver), FirElementAwareStringTable {
    override fun getLocalClassIdReplacement(firClass: FirClass): ClassId =
        components.classifierStorage.getCachedIrClass(firClass)?.getLocalClassIdReplacement()
            ?: throw AssertionError("not a local class: ${firClass.symbol.classId}")

    private fun IrClass.getLocalClassIdReplacement(): ClassId =
        when (val parent = parent) {
            is IrClass -> parent.getLocalClassIdReplacement().createNestedClassId(name)
            else -> {
                val fqName = FqName(typeMapper.mapClass(this).internalName.replace('/', '.'))
                ClassId(fqName.parent(), FqName.topLevel(fqName.shortName()), isLocal = true)
            }
        }
}
