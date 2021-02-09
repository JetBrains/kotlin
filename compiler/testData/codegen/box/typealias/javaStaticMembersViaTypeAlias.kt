// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: JTest.java
public class JTest {
    public static String o() { return "O"; }
    public static final String K = "K";
}

// MODULE: main(lib)
// FILE: 1.kt
typealias JT = JTest

fun box(): String = JT.o() + JT.K
