// !DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

expect fun foo1(x: Int)
expect fun foo2<!NO_ACTUAL_FOR_EXPECT{JVM}!>(x: Int)<!>

expect class NoArgConstructor()

expect fun foo3(): <!NO_ACTUAL_FOR_EXPECT{JVM}!>Int<!>
expect fun foo4<!NO_ACTUAL_FOR_EXPECT{JVM}!>()<!>: Int

// MODULE: m2-jvm()()(m1-common)

// FILE: jvm.kt

actual fun foo1(x: Int) {}

fun foo1(x: Int, y: Int) {}
fun foo1(x: String) {}

fun foo2(x: Int, y: Int) {}
fun foo2(x: String) {}

actual fun foo3(): <!ACTUAL_WITHOUT_EXPECT!>String<!> = ""
fun foo4(x: Int): String = ""

actual class NoArgConstructor {
    actual constructor()
    actual constructor<!ACTUAL_WITHOUT_EXPECT!>(x: Int)<!>
    constructor(x: String)
}
