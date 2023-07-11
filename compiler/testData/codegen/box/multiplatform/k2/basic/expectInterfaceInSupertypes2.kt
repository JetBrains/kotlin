// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// WITH_STDLIB
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect interface S1
expect interface S2

expect interface S : S1, S2

open class A : S

class B : A()

// MODULE: platform()()(common)
// FILE: platform.kt

@OptIn(ExperimentalMultiplatform::class)
@AllowDifferentMembersInActual
actual interface S1 {
    fun s1() = "O"
}

interface S20 {
    fun s2() = "K"
}

@OptIn(ExperimentalMultiplatform::class)
@AllowDifferentMembersInActual
actual interface S2 : S20 {
}

@OptIn(ExperimentalMultiplatform::class)
@AllowDifferentMembersInActual
actual interface S : S1, S2 {
    fun s3() = s1() + s2()
}

fun box() = B().s3()