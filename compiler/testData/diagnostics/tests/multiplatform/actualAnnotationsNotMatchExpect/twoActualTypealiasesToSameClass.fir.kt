// MODULE: m1-common
// FILE: common.kt
annotation class Ann

expect class WithAnn {
    @Ann
    fun foo()
}

expect class WithoutAnn {
    fun foo()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
class Impl {
    fun foo() {}
}

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>WithAnn<!> = Impl<!>
actual typealias WithoutAnn = Impl
