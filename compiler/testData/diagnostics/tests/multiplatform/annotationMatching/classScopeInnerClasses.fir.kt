// MODULE: m1-common
// FILE: common.kt
annotation class Ann

expect class A {
    class B {
        class C {
            @Ann
            fun foo()
        }
    }
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual class A {
    actual class B {
        actual class C {
            <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>foo<!>() {}<!>
        }
    }
}
