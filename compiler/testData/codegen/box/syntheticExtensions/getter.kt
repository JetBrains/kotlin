// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: JavaClass.java

class JavaClass {
    public String getOk() { return "OK"; }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    return JavaClass().ok
}
