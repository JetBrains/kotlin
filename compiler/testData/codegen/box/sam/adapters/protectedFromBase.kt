// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: JavaClass.java

class JavaClass<T> {
    protected void execute(T t, Runnable r) {
        r.run();
    }
}

// MODULE: main(lib)
// FILE: 1.kt

internal class KotlinClass : JavaClass<String>() {
    fun doIt(): String {
        var result = ""
        execute("") {
            result = "OK"
        }
        return result
    }
}

fun box(): String {
    return KotlinClass().doIt()
}
