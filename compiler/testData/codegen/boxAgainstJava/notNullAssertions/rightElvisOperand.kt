// IGNORE_BACKEND: JVM_IR
// FILE: RightElvisOperand.java

class RightElvisOperand {
    static String foo() {
        return null;
    }
}

// FILE: 1.kt

fun baz(): String? = null

fun bar(): String = baz() ?: RightElvisOperand.foo()

fun foo(x: String) {}

fun box(): String {
    try {
        foo(baz() ?: RightElvisOperand.foo())
        return "Fail: should have been an exception in `foo(baz() ?: RightElvisOperand.foo())`"
    }
    catch(e: IllegalStateException) {}

    try {
        bar()
        return "Fail: should have been an exception in `bar()`"
    }
    catch(e: IllegalStateException) {
        return "OK"
    }
}
