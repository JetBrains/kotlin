// !DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

expect open class Container {
    internal fun internalFun()
}

// MODULE: m2-jvm()()(m1-common)

// FILE: foo/Foo.java

package foo;

public class Foo {
    public final void internalFun() {}
}

// FILE: jvm.kt

actual typealias <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>Container<!> = foo.Foo
