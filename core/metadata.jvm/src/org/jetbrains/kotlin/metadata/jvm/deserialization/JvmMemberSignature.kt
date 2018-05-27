/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.jvm.deserialization

data class JvmMemberSignature(val name: String, val desc: String) {

    /**
     * Returns `true` when this signature represents a field.
     */
    val isField: Boolean = desc.indexOf(")") < 0

    /**
     * Returns `true` when this signature represents a method.
     */
    val isMethod: Boolean get() = !isField

    override fun toString() = if (isField) name + ":" + desc else name + desc
}