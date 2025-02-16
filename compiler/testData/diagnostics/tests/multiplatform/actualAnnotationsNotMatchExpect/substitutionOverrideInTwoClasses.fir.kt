// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
annotation class Ann

expect class WithAnn {
    @Ann
    fun foo(p: String)
}

expect class WithoutAnn {
    fun foo(p: String)
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
abstract class Parent<T> {
    open fun foo(p: T) {}
}

abstract class Intermediate : Parent<String>()

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> class WithAnn : Intermediate()

actual class WithoutAnn : Intermediate()
