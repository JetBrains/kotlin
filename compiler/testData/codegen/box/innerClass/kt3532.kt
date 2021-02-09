// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: Foo.java

public class Foo {
    public class Inner { }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    Foo().Inner()
    return "OK"
}
