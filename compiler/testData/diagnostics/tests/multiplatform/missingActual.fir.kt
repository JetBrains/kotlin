// ISSUE: KT-68830
// MODULE: m1-common
// FILE: common.kt

open expect class A() {
    open fun foo(): String
}

expect class B() : A

fun test() = B().foo()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

fun box(): String {
    test()
    return "OK"
}
