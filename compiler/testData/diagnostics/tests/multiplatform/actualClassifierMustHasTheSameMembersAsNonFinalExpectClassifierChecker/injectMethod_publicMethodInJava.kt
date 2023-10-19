// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect open class Foo {
    fun existingMethod()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias Foo = FooImpl

// FILE: Foo.java

public class FooImpl {
    public void existingMethod() {}

    public void injectedMethod() {}
}
