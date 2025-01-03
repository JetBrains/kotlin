// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> {
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
            actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>foo<!>() {}
        }
    }
}
