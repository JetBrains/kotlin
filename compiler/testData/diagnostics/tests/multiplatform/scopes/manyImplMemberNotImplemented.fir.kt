// TARGET_BACKEND: JVM
// LANGUAGE: +MultiPlatformProjects
// IGNORE_DIAGNOSTIC_API
// IGNORE_REVERSED_RESOLVE
//  Reason: MPP diagnostics are reported differentely in the compiler and AA

// MODULE: m1-common
// FILE: common.kt

expect open class C1()
expect interface I1

open <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class Common1_1<!> : C1(), I1
open <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class Common1_2<!> : I1, C1()

expect open class Expect1_1 : C1, I1
expect open class Expect1_2 : I1, C1


expect abstract class C2()
expect interface I2

open class Common2_1 : C2(), I2
open class Common2_2 : I2, C2()

expect open class Expect2_1 : C2, I2
expect open class Expect2_2 : I2, C2

// MODULE: m1-jvm()()(m1-common)
// FILE: main.kt

actual open class C1 {
    fun f() {}
}

actual interface I1 {
    fun f() {}
}

actual open <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class Expect1_1<!> : C1(), I1
actual open <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class Expect1_2<!> : I1, C1()


actual abstract class C2 actual constructor() {
    fun g() {}
}

actual interface I2 {
    fun g()
}

actual open class Expect2_1 : C2(), I2
actual open class Expect2_2 : I2, C2()
