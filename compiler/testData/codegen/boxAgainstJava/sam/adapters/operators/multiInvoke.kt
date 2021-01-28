// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: JavaClass.java

class JavaClass {
    void invoke(Runnable p1, Runnable p2) {
        p1.run();
        p2.run();
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    val obj = JavaClass()

    var v = "FAIL"
    obj({ v = "O" }, { v += "K" })
    return v
}
