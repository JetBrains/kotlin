// MODULE: m1-common
// FILE: common.kt

expect open class Foo {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

open class Injector {
    fun injectedMethod() {}
}

actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER!>class Foo<!> : Injector() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904
}
