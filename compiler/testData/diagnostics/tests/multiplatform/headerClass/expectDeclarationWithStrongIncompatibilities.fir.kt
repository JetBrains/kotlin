// !DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

expect fun foo1(x: Int)
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect fun foo2(x: Int)<!>

expect class NoArgConstructor()

expect fun foo3(): Int
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect fun foo4(): Int<!>

// MODULE: m2-jvm()()(m1-common)

// FILE: jvm.kt

actual fun foo1(x: Int) {}

fun foo1(x: Int, y: Int) {}
fun foo1(x: String) {}

fun foo2(x: Int, y: Int) {}
fun foo2(x: String) {}

<!ACTUAL_WITHOUT_EXPECT!>actual fun foo3(): String = ""<!>
fun foo4(x: Int): String = ""

actual class NoArgConstructor {
    actual constructor()
    <!ACTUAL_WITHOUT_EXPECT!>actual constructor(x: Int)<!>
    constructor(x: String)
}
