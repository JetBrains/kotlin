/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

typealias ExtendedWhenMissingCase = List<WhenMissingCaseFor>

data class WhenMissingCaseFor(
    val expression: KtSourceElement?,
    val missingCase: WhenMissingCase
)

sealed class WhenMissingCase {
    abstract val branchConditionText: String
    open val reportAsEquality: Boolean = false

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
        override val reportAsEquality: Boolean = true
    }

    object NotNullIsMissing : WhenMissingCase() {
        override val branchConditionText: String = "!= null"
    }

    sealed class BooleanIsMissing(val value: Boolean) : WhenMissingCase() {
        object TrueIsMissing : BooleanIsMissing(true)
        object FalseIsMissing : BooleanIsMissing(false)

        override val branchConditionText: String = value.toString()
        override val reportAsEquality: Boolean = true
    }

    class IsTypeCheckIsMissing(val classId: ClassId, val isSingleton: Boolean) : WhenMissingCase() {
        override val branchConditionText: String = run {
            val fqName = classId.asSingleFqName().toString()
            if (isSingleton) fqName else "is $fqName"
        }

        override val reportAsEquality: Boolean = isSingleton

        override fun toString(): String {
            val className = classId.shortClassName
            val name = if (className.isSpecial) className.asString() else className.identifier
            return if (isSingleton) name else "is $name"
        }
    }

    class EnumCheckIsMissing(val callableId: CallableId) : WhenMissingCase() {
        override val branchConditionText: String = callableId.asSingleFqName().toString()
        override val reportAsEquality: Boolean = true

        override fun toString(): String {
            return callableId.callableName.identifier
        }
    }

    override fun toString(): String {
        return branchConditionText
    }
}
