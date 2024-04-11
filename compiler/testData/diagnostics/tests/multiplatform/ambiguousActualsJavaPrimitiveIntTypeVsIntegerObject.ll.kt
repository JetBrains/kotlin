// MODULE: m1-common
// FILE: common.kt

expect class Foo {
    fun push(value: Int)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias Foo = FooImpl

// FILE: FooImpl.java
public class FooImpl {
    public void push(int value) {}
    public void push(Integer value) {}
}
