// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt

expect class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Foo<!>() {
    fun foo()
    override fun equals(other: Any?): Boolean
    class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Nested<!>
    inner class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Inner<!>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

public class Foo {
    public void foo() {
    }
    public static class Nested {}
    public class Inner {}
}
