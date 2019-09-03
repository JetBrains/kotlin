@file:Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")
package sample

interface D : A {
    fun foo_B_1()
    fun foo_B_2()
    fun foo_C_1()
    fun foo_C_2()
    fun foo_D()
}

actual typealias B = D
actual typealias C = D

fun take0(x: D): Unit = null!!

fun test_1(x: D) {
    val res0: Unit = take0(x)
    <!OVERLOAD_RESOLUTION_AMBIGUITY(" public fun take1(x: A): AA defined in sample in file common-1.kt public fun take1(x: A): DD defined in sample in file common-2-3.kt public fun take1(x: B): BB defined in sample in file common-2-1.kt public fun take1(x: C): CC defined in sample in file common-2-2.kt")!>take1<!>(x)
    val res2: BB = take2(x)
    val res3: BB = take3(x)
    <!OVERLOAD_RESOLUTION_AMBIGUITY(" public fun take4(x: A): AA defined in sample in file common-1.kt public fun take4(x: A): DD defined in sample in file common-2-3.kt")!>take4<!>(x)
}

fun test_2() {
    val x = getB()
    x.foo_A()
    x.foo_A_3()
    x.foo_B_1()
    x.foo_B_2()
    x.foo_C_1()
    x.foo_C_2()
    x.foo_D()
}

fun test_3() {
    val x = getB()
    x.foo_A()
    x.foo_A_3()
    x.foo_B_1()
    x.foo_B_2()
    x.foo_C_1()
    x.foo_C_2()
    x.foo_D()
}