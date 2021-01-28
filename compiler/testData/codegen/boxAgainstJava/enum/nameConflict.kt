// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: C.java
public enum C {
    OK(0);

    private final int ordinal;

    C(int ordinal) { this.ordinal = ordinal; }

    public int getOrdinal() { return ordinal; }
}

// MODULE: main(lib)
// FILE: 1.kt
fun box(): String {
    val ok = C.OK
    return when (ok) {
        C.OK -> "OK"
        else -> "fail"
    }
}
