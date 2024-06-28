// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class Foo()
expect class Foo2()

expect class Bar
expect class Bar2

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo<!ACTUAL_MISSING!>()<!>
actual class Foo2 {
    <!ACTUAL_MISSING!>constructor()<!>
}

actual class Bar()
actual class Bar2 {
    constructor()
}
