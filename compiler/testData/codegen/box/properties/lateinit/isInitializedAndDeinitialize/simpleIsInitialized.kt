// TARGET_BACKEND: JVM
// LANGUAGE_VERSION: 1.2
// WITH_RUNTIME

// FILE: J.java

public class J {
    public static void deinitialize(Foo foo) {
        foo.bar = null;
    }
}

// FILE: main.kt

class Foo {
    lateinit var bar: String

    fun test(): String {
        if (this::bar.isInitialized) return "Fail 1"
        J.deinitialize(this)
        if (this::bar.isInitialized) return "Fail 2"

        bar = "A"
        if (!this::bar.isInitialized) return "Fail 3"
        J.deinitialize(this)
        if (this::bar.isInitialized) return "Fail 4"

        bar = "OK"
        if (!this::bar.isInitialized) return "Fail 5"
        return bar
    }
}

fun box(): String {
    return Foo().test()
}
