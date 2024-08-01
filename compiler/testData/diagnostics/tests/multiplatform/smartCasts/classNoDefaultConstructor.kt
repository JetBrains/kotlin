// ISSUE: KT-61506
// MODULE: m1-common
// FILE: common.kt

package pack

expect class Bar {
    fun foo(): String
}

fun testCommon() {
    <!RESOLUTION_TO_CLASSIFIER!>Bar<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

package pack

actual class Bar {
    actual fun foo() = "expect class fun: jvm"
}

fun testPlatform() {
    Bar().foo()
}