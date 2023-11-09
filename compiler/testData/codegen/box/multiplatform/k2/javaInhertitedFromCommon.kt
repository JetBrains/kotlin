// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// !LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// CHECK_BYTECODE_LISTING

// MODULE: common
// FILE: common.kt

expect class A

abstract class Base() {
    abstract fun foo(x: A) : Any
}

abstract class Derived : Base() {
}


// MODULE: platform()()(common)
// FILE: Impl.java

public class Impl extends Derived {
    public Integer foo(int x) { return x; }
}

// FILE: platform.kt

class KotlinImpl: Impl()

actual typealias A = Int


fun box(): String {
    if ((KotlinImpl() as Base).foo(1) != 1) return "FAIL"
    if ((KotlinImpl() as Derived).foo(1) != 1) return "FAIL"
    if ((KotlinImpl() as Impl).foo(1) != 1) return "FAIL"
    if (KotlinImpl().foo(1) != 1) return "FAIL"
    return "OK"
}
