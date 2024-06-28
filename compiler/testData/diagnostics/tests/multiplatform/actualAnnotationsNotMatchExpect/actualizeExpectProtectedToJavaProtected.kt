// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR JVM
// ISSUE: KT-66436

// MODULE: common
// FILE: common.kt
package pkg

expect open class <!NO_ACTUAL_FOR_EXPECT!>Foo<!> {
    protected fun foo()
}

fun common(foo: Foo) {
    foo.<!INVISIBLE_MEMBER!>foo<!>()
}

// MODULE: jvm()()(common)
// FILE: pkg/FooImpl.java
package pkg;

public class FooImpl {
    protected void foo() {}
}

// FILE: jvm.kt
package pkg

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> = FooImpl

fun jvm(foo: Foo) {
    foo.foo()
}
