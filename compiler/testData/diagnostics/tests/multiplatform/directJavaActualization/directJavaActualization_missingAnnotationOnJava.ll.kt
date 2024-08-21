// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt

expect class Foo() {
    fun foo()
    override fun equals(other: Any?): Boolean
    class Nested
    inner class Inner
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

public class Foo {
    public void foo() {
    }
    public static class Nested {}
    public class Inner {}
}
