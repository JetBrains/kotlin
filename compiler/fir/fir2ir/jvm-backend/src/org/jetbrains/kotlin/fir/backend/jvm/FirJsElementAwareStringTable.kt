/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.serialization.FirElementAwareStringTable
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.metadata.jvm.serialization.SimpleStringTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class FirJsElementAwareStringTable(
    private val components: Fir2IrComponents,
) : SimpleStringTable(), FirElementAwareStringTable {
    override fun getLocalClassIdReplacement(firClass: FirClass): ClassId =
        components.classifierStorage.getCachedIrClass(firClass)?.getLocalClassIdReplacement()
            ?: throw AssertionError("not a local class: ${firClass.symbol.classId}")

    private fun IrClass.getLocalClassIdReplacement(): ClassId =
        when (val parent = parent) {
            is IrClass -> parent.getLocalClassIdReplacement().createNestedClassId(name)
            else -> {
                val fqName = this.kotlinFqName
                ClassId(fqName.parent(), FqName.topLevel(fqName.shortName()), true)
            }
        }
}
