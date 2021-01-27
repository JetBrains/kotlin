// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: Base.java

interface Base<T> {
    void call(T t);
}

// FILE: Derived.java

interface Derived extends Base<String> {
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    Base<String>{}.call("")
    Derived{}.call("")
    return "OK"
}
