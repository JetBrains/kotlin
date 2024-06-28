// MODULE: m1-common
// FILE: common.kt
expect interface Foo {
    fun foo(p: Int = 1)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
interface Base1 {
    fun foo(p: Int)
}

interface Base2 {
    fun foo(p: Int)
}

@Suppress("ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING")
actual interface Foo : Base1, Base2
