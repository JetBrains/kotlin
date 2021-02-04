// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: J.java

public class J {
    protected final String protectedProperty;

    public J(String str) {
        protectedProperty = str;
    }

    protected static String protectedFun() {
        return "OK";
    }
}

// MODULE: main(lib)
// FILE: 1.kt

class A : J(J.protectedFun()) {
    fun test(): String {
        return protectedProperty!!
    }
}

fun box(): String {
    return A().test()
}
