// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: protectedPack/J.java

package protectedPack;

public class J {
    protected static class Inner {
        public String foo() {
            return "OK";
        }
    }
}

// MODULE: main(lib)
// FILE: 1.kt

package protectedPack

class Derived : J() {
    fun test(): String {
        return J.Inner().foo()!!
    }
}

fun box(): String {
    return Derived().test()
}
