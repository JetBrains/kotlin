// RUN_PIPELINE_TILL: FIR2IR
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt

<!KOTLIN_ACTUAL_ANNOTATION_MISSING{JVM}!>expect<!> class Foo<!KOTLIN_ACTUAL_ANNOTATION_MISSING{JVM}!>()<!> {
    fun <!KOTLIN_ACTUAL_ANNOTATION_MISSING{JVM}!>foo<!>()
    override fun equals(other: Any?): Boolean
    class <!KOTLIN_ACTUAL_ANNOTATION_MISSING{JVM}!>Nested<!>
    inner class <!KOTLIN_ACTUAL_ANNOTATION_MISSING{JVM}!>Inner<!>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

public class Foo {
    public void foo() {
    }
    public static class Nested {}
    public class Inner {}
}
