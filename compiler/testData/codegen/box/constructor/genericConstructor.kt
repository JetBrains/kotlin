// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: test/Foo.java

package test;

public class Foo<T extends Number> {
    public Foo(T number) {}
}

// MODULE: main(lib)
// FILE: 1.kt

import test.Foo

class Subclass : Foo<Int>(42) {
}

fun box(): String {
    Subclass()
    return "OK"
}
