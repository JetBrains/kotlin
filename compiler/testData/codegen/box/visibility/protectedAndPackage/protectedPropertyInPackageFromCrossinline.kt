// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: protectedPack/J.java

package protectedPack;

public class J {
    protected String foo = "OK";
}

// MODULE: main(lib)
// FILE: 1.kt

package protectedPack

inline fun foo(crossinline bar: () -> String) = object {
    fun baz() = bar()
}.baz()

fun box(): String {
    return foo { J().foo!! }
}
