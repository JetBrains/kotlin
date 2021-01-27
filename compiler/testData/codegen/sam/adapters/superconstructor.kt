// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: JavaClass.java

class JavaClass {
    JavaClass(Runnable r) {
        if (r != null) r.run();
    }
}

// MODULE: main(lib)
// FILE: 1.kt

internal class KotlinClass(): JavaClass(null) {
}

fun box(): String {
    KotlinClass()
    return "OK"
}
