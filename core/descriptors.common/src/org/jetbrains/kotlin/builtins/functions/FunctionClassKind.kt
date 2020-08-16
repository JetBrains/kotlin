/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.functions

import org.jetbrains.kotlin.builtins.KotlinBuiltInsNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

enum class FunctionClassKind(val packageFqName: FqName, val classNamePrefix: String) {
    Function(KotlinBuiltInsNames.BUILT_INS_PACKAGE_FQ_NAME, "Function"),
    SuspendFunction(KotlinBuiltInsNames.COROUTINES_PACKAGE_FQ_NAME_RELEASE, "SuspendFunction"),
    KFunction(KotlinBuiltInsNames.KOTLIN_REFLECT_FQ_NAME, "KFunction"),
    KSuspendFunction(KotlinBuiltInsNames.KOTLIN_REFLECT_FQ_NAME, "KSuspendFunction");

    fun numberedClassName(arity: Int) = Name.identifier("$classNamePrefix$arity")

    companion object {
        fun byClassNamePrefix(packageFqName: FqName, className: String): FunctionClassKind? =
            values().firstOrNull { it.packageFqName == packageFqName && className.startsWith(it.classNamePrefix) }
    }
}
