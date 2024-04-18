// ALLOW_KOTLIN_PACKAGE
// STDLIB_COMPILATION
// ISSUE: KT-65841

// MODULE: m1-common
// FILE: common.kt

// FILE: internal.kt

package kotlin.internal

internal annotation class ActualizeByJvmBuiltinProvider()

// FILE: builtins.kt

@file:Suppress("INVISIBLE_REFERENCE")

package kotlin

import kotlin.internal.ActualizeByJvmBuiltinProvider
import kotlin.internal.PureReifiable

@ActualizeByJvmBuiltinProvider
expect open class <!NO_ACTUAL_FOR_EXPECT{JVM}!>Any<!>() {
    public open operator fun equals(other: Any?): Boolean

    fun funWithoutActual()
}

@ActualizeByJvmBuiltinProvider
expect class <!NO_ACTUAL_FOR_EXPECT{JVM}!>Boolean<!>

// Check that `ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT` (missing `@PureReifiable`) warning doesn't cause crash
@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
@ActualizeByJvmBuiltinProvider
public expect fun <reified @<!INVISIBLE_MEMBER, INVISIBLE_MEMBER{JVM}!>PureReifiable<!> T> <!NO_ACTUAL_FOR_EXPECT{JVM}!>arrayOfNulls<!>(size: Int): Array<T?>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
