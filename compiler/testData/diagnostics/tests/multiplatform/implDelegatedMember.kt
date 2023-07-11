// MODULE: m1-common
// FILE: common.kt

expect open class Foo {
    open fun bar(): String
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

interface Bar {
    fun bar(): String
}

val bar: Bar
    get() = null!!

actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER!>class Foo<!> : Bar by bar
