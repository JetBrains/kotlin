// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: test/Foo.java

package test;

public class Foo {
    protected void foo(Runnable r) {
        r.run();
    }
}

// MODULE: main(lib)
// FILE: test.kt

package other

import test.Foo

class Bar : Foo() {
    fun bar() {
        foo {}
        foo(Runnable {})
        // super.foo {}
        super.foo(Runnable {})
        this.foo {}
        this.foo(Runnable {})
    }
}

fun box(): String {
    Bar().bar()
    return "OK"
}
