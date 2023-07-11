// MODULE: m1-common
// FILE: common.kt

expect open class Foo {
    fun existingMethod()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>Foo<!> = FooImpl

// FILE: Foo.java

public class FooImpl {
    public void existingMethod() {}

    public void injectedMethod() {}
}
