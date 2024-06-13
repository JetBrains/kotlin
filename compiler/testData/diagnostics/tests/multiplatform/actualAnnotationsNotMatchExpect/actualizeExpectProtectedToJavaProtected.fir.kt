// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR JVM
// ISSUE: KT-66436

// MODULE: common
// FILE: common.kt
package pkg

expect open class Foo {
    protected fun foo()
}

fun common(foo: Foo) {
    foo.<!INVISIBLE_REFERENCE!>foo<!>()
}

// MODULE: jvm()()(common)
// FILE: pkg/FooImpl.java
package pkg;

public class FooImpl {
    protected void foo() {}
}

// FILE: jvm.kt
package pkg

actual typealias Foo = FooImpl

fun jvm(foo: Foo) {
    foo.foo()
}
