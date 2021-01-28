// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: JavaClass.java

class JavaClass {
    public static String run(Runnable r) {
        return r == null ? "OK" : "FAIL";
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    val f: (() -> Unit)? = null
    return JavaClass.run(f)!!
}
