// LANGUAGE: +MultiPlatformProjects
// OPT_IN: kotlin.ExperimentalMultiplatform
// WITH_STDLIB
// ISSUE: KT-69024

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: expected.kt

package a

@OptionalExpectation
expect annotation class A(val x: Int)

@OptionalExpectation
expect annotation class B(val s: String) {
    @OptionalExpectation
    annotation class C(val a: Boolean)
}

// MODULE: library()()(common)
// FILE: library.kt

package a

actual annotation class A(actual val x: Int)

// MODULE: common2
// TARGET_PLATFORM: Common
// FILE: common2.kt

package usage

import a.B

@B("OK")
@B.C(true)
fun ok() = "OK"

expect annotation class C()

@OptionalExpectation
expect annotation class AnnWithOptionalExpectation(val c: C) {
    annotation class NestedAnnWithOptionalExpectation(val c2: C)
}

// MODULE: main(library)()(common2)
// FILE: main.kt

package usage

import a.A

actual annotation class C()

@A(42)
fun box(): String = ok()
