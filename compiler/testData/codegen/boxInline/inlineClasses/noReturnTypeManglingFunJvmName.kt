// !LANGUAGE: +InlineClasses -MangleClassMembersReturningInlineClasses
// WITH_STDLIB
// TARGET_BACKEND: JVM

// FILE: 1.kt

package test

inline class S(val string: String)

@Suppress("INAPPLICABLE_JVM_NAME")
@JvmName("foo")
inline fun foo() = S("OK")

// FILE: 2.kt

import test.*

fun box() : String =
    foo().string
