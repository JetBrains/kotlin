// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: JavaClass.java

class JavaClass {
    private Runnable r;

    public JavaClass(Runnable r) {
        this.r = r;
    }

    public void run() {
        r.run();
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    var v = "FAIL"
    val f = { v = "OK" }
    JavaClass(f).run()
    return v
}
