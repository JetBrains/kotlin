/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.renderer.render

sealed class WhenMissingCase {
    abstract val branchConditionText: String

    object Unknown : WhenMissingCase() {
        override fun toString(): String = "unknown"

        override val branchConditionText: String = "else"
    }

    sealed class ConditionTypeIsExpect(val typeOfDeclaration: String) : WhenMissingCase() {
        object SealedClass : ConditionTypeIsExpect("sealed class")
        object SealedInterface : ConditionTypeIsExpect("sealed interface")
        object Enum : ConditionTypeIsExpect("enum")

        override val branchConditionText: String = "else"

        override fun toString(): String = "unknown"
    }

    object NullIsMissing : WhenMissingCase() {
        override val branchConditionText: String = "null"
    }

    sealed class BooleanIsMissing(val value: Boolean) : WhenMissingCase() {
        object TrueIsMissing : BooleanIsMissing(true)
        object FalseIsMissing : BooleanIsMissing(false)

        override val branchConditionText: String = value.toString()
    }

    class IsTypeCheckIsMissing(val classId: ClassId, val isSingleton: Boolean, val ownTypeParametersCount: Int) : WhenMissingCase() {
        override val branchConditionText: String = run {
            val fqName = classId.asSingleFqName().render()
            val type = "$fqName$typeArguments"
            if (isSingleton) type else "is $type"
        }

        override fun toString(): String {
            val shortName = classId.shortClassName.render()
            val type = "$shortName$typeArguments"
            return if (isSingleton) type else "is $type"
        }

        private val typeArguments: String
            get() = if (ownTypeParametersCount != 0) {
                CharArray(ownTypeParametersCount) { '*' }.joinToString(prefix = "<", postfix = ">")
            } else ""
    }

    class EnumCheckIsMissing(val callableId: CallableId) : WhenMissingCase() {
        override val branchConditionText: String = callableId.asSingleFqName().render()

        override fun toString(): String {
            return callableId.callableName.render()
        }
    }

    override fun toString(): String {
        return branchConditionText
    }
}
