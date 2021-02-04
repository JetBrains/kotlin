// IGNORE_BACKEND_FIR: JVM_IR
// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// SKIP_JDK6
// WITH_RUNTIME
// MODULE: lib
// FILE: JavaClass.java

class JavaClass {
    interface Computable<T> {
        T compute();
    }

    static <T> T compute(Computable<T> computable) {
        return computable.compute();
    }
}

// MODULE: main(lib)
// FILE: 1.kt

import java.util.Arrays

fun box(): String {
    val r: JavaClass.Computable<String> = JavaClass.Computable { "OK" }
    val supertypes = Arrays.toString(r.javaClass.getGenericInterfaces())
    if (supertypes != "[JavaClass\$Computable<java.lang.String>]") return "Fail: $supertypes"
    return JavaClass.compute(r)!!
}
