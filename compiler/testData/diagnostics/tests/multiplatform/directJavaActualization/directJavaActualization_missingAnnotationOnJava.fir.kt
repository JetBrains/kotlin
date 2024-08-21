// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt

<!KOTLIN_ACTUAL_ANNOTATION_MISSING{JVM}!>expect class Foo<!KOTLIN_ACTUAL_ANNOTATION_MISSING{JVM}!>()<!> {
    <!KOTLIN_ACTUAL_ANNOTATION_MISSING{JVM}!>fun foo()<!>
    override fun equals(other: Any?): Boolean
    <!KOTLIN_ACTUAL_ANNOTATION_MISSING{JVM}!>class Nested<!>
    <!KOTLIN_ACTUAL_ANNOTATION_MISSING{JVM}!>inner class Inner<!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

public class Foo {
    public void foo() {
    }
    public static class Nested {}
    public class Inner {}
}
