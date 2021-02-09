// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: JavaClass.java

class JavaClass {
    public void run(Runnable r) {
        r.run();
    }
}

// MODULE: main(lib)
// FILE: 1.kt

internal class KotlinSubclass: JavaClass() {
}

fun box(): String {
    var v = "FAIL"
    KotlinSubclass().run { v = "OK" }
    return v
}
