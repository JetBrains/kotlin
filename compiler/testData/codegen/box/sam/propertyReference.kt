// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: Base.java

interface Interface {
    String call(String t);
}

// MODULE: main(lib)
// FILE: 1.kt

val String.property: String
    get() = this

fun box(): String {
    return Interface(String::property).call("OK")
}
