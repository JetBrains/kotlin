// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-66436
// DUMP_IR

// MODULE: common
// FILE: common.kt
package pkg

expect open class Foo constructor() {
    protected fun foo(): String
}

class Bar : Foo() {
    fun bar() = foo()
}

// MODULE: jvm()()(common)
// FILE: pkg/FooImpl.java
package pkg;

public class FooImpl {
    protected String foo() {
        return "Hello from Java";
    }
}

// FILE: jvm.kt
package pkg

actual typealias Foo = FooImpl

fun box(): String {
    val result = Bar().bar()
    return if (result == "Hello from Java") "OK" else result
}
