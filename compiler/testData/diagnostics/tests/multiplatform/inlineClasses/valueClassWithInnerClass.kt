// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect value class <!NO_ACTUAL_FOR_EXPECT!>A<!>(val s: String)

expect value class <!NO_ACTUAL_FOR_EXPECT!>B<!>(val s: String)

open class C {
    open fun foo() = ""
}

// MODULE: jvm()()(common)
// FILE: jvm.kt
@JvmInline
actual value class A(val s: String) {
    <!INNER_CLASS_INSIDE_VALUE_CLASS!>inner<!> class D
}

@JvmInline
actual value class B(val s: String) {
    val x
        get() = object : C() {
            override fun foo() = s
        }
}
