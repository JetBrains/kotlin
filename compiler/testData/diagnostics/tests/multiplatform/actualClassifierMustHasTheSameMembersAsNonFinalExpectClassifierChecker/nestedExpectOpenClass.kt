// MODULE: m1-common
// FILE: common.kt

// Rules for expect actual matching are ad-hoc for nested classes. That's why this test exist
expect class Outer {
    open class Foo {
        fun existingMethod()
        val existingParam: Int
    }
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Outer {
    actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>class Foo<!> {
        actual fun existingMethod() {}
        actual val existingParam: Int = 904

        fun <!NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION!>injectedMethod<!>() {} // accidential override can happen with this injected fun. That's why it's prohibited
    }
}
