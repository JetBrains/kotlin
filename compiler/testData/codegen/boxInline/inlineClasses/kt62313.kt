// !LANGUAGE: +InlineClasses +MangleClassMembersReturningInlineClasses

// FILE: 1.kt

package test

inline class S(val string: String)

inline fun foo() = S("OK")

// FILE: 2.kt

import test.*

inline fun bar(f: () -> S): S = f()

fun box() : String =
    bar(::foo).string
