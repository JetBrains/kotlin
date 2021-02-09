// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: ParamBase.java

class ParamBase<T> {}

// FILE: F.java

interface F<T> extends FBase<ParamBase<? extends T>> {
}

// FILE: FBase.java

interface FBase<T> {
    void call(T t);
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    F<ParamBase<Int>>({}).call(ParamBase())
    return "OK"
}
