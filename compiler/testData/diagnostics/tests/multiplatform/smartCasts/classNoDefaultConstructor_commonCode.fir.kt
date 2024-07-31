// ISSUE: KT-61506
// MODULE: m1-common
// FILE: common.kt

package pack

expect class Bar {
    fun foo(): String
}

fun common() {
    <!EXPECT_CLASS_AS_FUNCTION!>Bar<!>().<!UNRESOLVED_REFERENCE!>foo<!>()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

package pack

actual class Bar {
    actual fun foo() = "expect class fun: jvm"
}