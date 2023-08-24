// MODULE: m1-common
// FILE: common.kt

expect open class Foo {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>Foo<!> = FooImpl

open class FooImpl {
    fun existingMethod() {}
    val existingParam: Int = 904

    fun injectedMethod() {} // accidential override can happen with this injected fun. That's why it's prohibited
}
