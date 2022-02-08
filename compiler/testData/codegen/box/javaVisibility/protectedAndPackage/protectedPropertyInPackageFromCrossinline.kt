// TARGET_BACKEND: JVM
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
