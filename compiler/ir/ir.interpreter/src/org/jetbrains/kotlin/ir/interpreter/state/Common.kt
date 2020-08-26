/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.interpreter.isInterface
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization

internal class Common private constructor(
    override val irClass: IrClass, override val fields: MutableList<Variable>
) : Complex(irClass, fields) {

    constructor(irClass: IrClass) : this(irClass, mutableListOf())

    fun setSuperClassRecursive() {
        var thisClass: Common? = this
        while (thisClass != null) {
            val superClass = thisClass.irClass.superTypes.filterNot { it.isInterface() }.singleOrNull()
            val superClassOwner = superClass?.classOrNull?.owner
            val superClassState = superClassOwner?.let { Common(it) }
            superClassState?.let { thisClass!!.setSuperClassInstance(it) }

            if (superClass == null && thisClass.irClass.superTypes.isNotEmpty()) {
                // cover the case when super type implement an interface and so doesn't have explicit any as super class
                thisClass.setSuperClassInstance(Common(getAnyClassRecursive()))
            }
            thisClass = superClassState
        }
    }

    private fun getAnyClassRecursive(): IrClass {
        var owner = irClass.superTypes.first().classOrNull!!.owner
        while (owner.superTypes.isNotEmpty()) owner = owner.superTypes.first().classOrNull!!.owner
        return owner
    }

    override fun toString(): String {
        return "Common(obj='${irClass.fqNameForIrSerialization}', super=$superClass, values=$fields)"
    }
}