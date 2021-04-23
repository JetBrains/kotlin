/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.functions

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

enum class FunctionClassKind(
    val packageFqName: FqName,
    val classNamePrefix: String,
    val isSuspendType: Boolean,
    val isReflectType: Boolean
) {
    Function(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, "Function", isSuspendType = false, isReflectType = false),
    SuspendFunction(StandardNames.COROUTINES_PACKAGE_FQ_NAME_RELEASE, "SuspendFunction", isSuspendType = true, isReflectType = false),
    KFunction(StandardNames.KOTLIN_REFLECT_FQ_NAME, "KFunction", isSuspendType = false, isReflectType = true),
    KSuspendFunction(StandardNames.KOTLIN_REFLECT_FQ_NAME, "KSuspendFunction", isSuspendType = true, isReflectType = true);

    fun numberedClassName(arity: Int) = Name.identifier("$classNamePrefix$arity")

    companion object {
        fun byClassNamePrefix(packageFqName: FqName, className: String): FunctionClassKind? =
            values().firstOrNull { it.packageFqName == packageFqName && className.startsWith(it.classNamePrefix) }

        data class KindWithArity(val kind: FunctionClassKind, val arity: Int)

        fun parseClassName(className: String, packageFqName: FqName): KindWithArity? {
            val kind = byClassNamePrefix(packageFqName, className) ?: return null

            val prefix = kind.classNamePrefix

            val arity = toInt(className.substring(prefix.length)) ?: return null

            // TODO: validate arity, should be <= 255
            return KindWithArity(kind, arity)
        }

        @JvmStatic
        fun getFunctionalClassKind(className: String, packageFqName: FqName) =
            parseClassName(className, packageFqName)?.kind

        private fun toInt(s: String): Int? {
            if (s.isEmpty()) return null

            var result = 0
            for (c in s) {
                val d = c - '0'
                if (d !in 0..9) return null
                result = result * 10 + d
            }
            return result
        }
    }
}
