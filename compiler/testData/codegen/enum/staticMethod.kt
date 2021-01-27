// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: test/En.java

package test;

public enum En {
    ENTRY;
    
    public static String foo() {
        return "OK";
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box() = test.En.foo()
