// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: RightElvisOperand.java

class RightElvisOperand {
    static String foo() {
        return null;
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun baz(): String? = null

fun bar(): String = baz() ?: RightElvisOperand.foo()

fun foo(x: String) {}

fun box(): String {
    try {
        foo(baz() ?: RightElvisOperand.foo())
        return "Fail: should have been an exception in `foo(baz() ?: RightElvisOperand.foo())`"
    }
    catch(e: NullPointerException) {}

    try {
        bar()
        return "Fail: should have been an exception in `bar()`"
    }
    catch(e: NullPointerException) {
        return "OK"
    }
}
