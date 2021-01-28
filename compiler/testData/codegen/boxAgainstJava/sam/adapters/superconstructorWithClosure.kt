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

var status: String = "fail"  // global property to avoid issues with accessing closure from local class (KT-4174)

internal class KotlinClass(): JavaClass({status="OK"}) {
}

fun box(): String {
    KotlinClass()
    return status
}
