// MODULE: m1-common
// FILE: common.kt

expect class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Foo<!>() {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

public class Foo {
    public void foo() {
    }
}
