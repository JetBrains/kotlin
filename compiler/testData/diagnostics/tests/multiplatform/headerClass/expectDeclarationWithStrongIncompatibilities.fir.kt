// !DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

expect fun foo1(x: Int)
<!EXPECT_ACTUAL_MISMATCH{JVM}, EXPECT_ACTUAL_MISMATCH{JVM}!>expect fun foo2(x: Int)<!>

expect class NoArgConstructor()

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect fun foo3(): Int<!>
<!EXPECT_ACTUAL_MISMATCH{JVM}!>expect fun foo4(): Int<!>

// MODULE: m2-jvm()()(m1-common)

// FILE: jvm.kt

actual fun foo1(x: Int) {}

fun foo1(x: Int, y: Int) {}
fun foo1(x: String) {}

fun foo2(x: Int, y: Int) {}
fun foo2(x: String) {}

actual fun <!ACTUAL_WITHOUT_EXPECT!>foo3<!>(): String = ""
fun foo4(x: Int): String = ""

actual class NoArgConstructor {
    actual constructor()
    <!ACTUAL_WITHOUT_EXPECT!>actual constructor(x: Int)<!>
    constructor(x: String)
}
