/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// NB: with className == null we are at top level
data class CallableId(val packageName: FqName, val className: FqName?, val callableName: Name) {
    val classId: ClassId? get() = className?.let { ClassId(packageName, it, false) }

    constructor(classId: ClassId, callableName: Name) : this(classId.packageFqName, classId.relativeClassName, callableName)

    constructor(packageName: FqName, callableName: Name) : this(packageName, null, callableName)

    @Deprecated("TODO: Better solution for local callables?")
    constructor(callableName: Name) : this(FqName.topLevel(Name.special("<local>")), null, callableName)


    override fun toString(): String {
        return buildString {
            append(packageName.asString().replace('.', '/'))
            append("/")
            if (className != null) {
                append(className)
                append(".")
            }
            append(callableName)
        }
    }
}


