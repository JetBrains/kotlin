// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: Sub.java

class Super {
    void safeInvoke(Runnable r) {
        if (r != null) r.run();
    }
}

class Sub extends Super {
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    var r = "FAIL"
    val sub = Sub()
    sub.safeInvoke(null)
    sub.safeInvoke { r = "OK" }
    return r
}
