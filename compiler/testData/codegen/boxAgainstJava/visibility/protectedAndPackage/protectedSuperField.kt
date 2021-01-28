// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: test/Foo.java
package test;

public class Foo {
    protected final String value;

    protected Foo(String value) {
        this.value = value;
    }
}

// MODULE: main(lib)
// FILE: test.kt
import test.Foo

class Bar : Foo("OK") {
    fun baz() = super.value
}

fun box(): String = Bar().baz()
