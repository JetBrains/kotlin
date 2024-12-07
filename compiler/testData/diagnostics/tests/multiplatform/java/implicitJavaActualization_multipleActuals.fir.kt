// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> class Foo(i: Int) {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

public class Foo {
    public Foo(int i) {}
    public void foo() {}
}

// FILE: jvm.kt

class <!ACTUAL_MISSING, CLASSIFIER_REDECLARATION!>Foo<!><T>(t: T) {
    fun <!ACTUAL_MISSING!>foo<!>() {}
}
