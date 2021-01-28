// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: R.java

class R {
    public static class id {
        public static final int main = 17;
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box() =
    if (R.id.main == 17) "OK" else "fail"
