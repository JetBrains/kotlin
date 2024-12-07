// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> class Foo {
    var <!EXPECT_ACTUAL_MISMATCH{JVM}!>foo<!>: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> = JavaFoo

// FILE: JavaFoo.java
public class JavaFoo {
    public int foo = 0;
}
