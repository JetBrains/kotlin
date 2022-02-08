// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: Foo.java

import java.util.Collection;

public class Foo {
    public static <T extends CharSequence & java.io.Serializable> T id(T p) {
        return p;
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    return Foo.id("OK" as java.lang.String)!! as kotlin.String
}
