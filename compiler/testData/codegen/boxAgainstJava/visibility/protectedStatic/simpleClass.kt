// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: Base.java

public class Base {
    protected static class Inner {
        public Inner() {}
        public String foo() {
            return "OK";
        }
    }
}

// MODULE: main(lib)
// FILE: 1.kt

class Derived : Base() {
    fun test(): String {
        return Base.Inner().foo()!!
    }
}

fun box(): String {
    return Derived().test()
}
