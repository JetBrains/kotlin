// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class Foo {
    <!EXPECT_ACTUAL_MISMATCH{JVM}!>var foo: Int<!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> = JavaFoo

// FILE: JavaFoo.java
public class JavaFoo {
    public int foo = 0;
}
