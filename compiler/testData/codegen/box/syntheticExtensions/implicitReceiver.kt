// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: JavaClass.java

class JavaClass {
    private String myX;

    public String getX() {
        return myX;
    }

    public void setX(String x) {
        myX = x;
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    return JavaClass().doIt()
}

internal fun JavaClass.doIt(): String {
    x = "OK"
    return x
}
