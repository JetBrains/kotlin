// TARGET_BACKEND: JVM
// FILE: B.kt

class B : C() {
    fun test(): String = bar(this::foo)
}

fun bar(f: () -> String): String = f()

fun box(): String = B().test()

// FILE: C.java

public class C {
    protected final String foo() {
        return "OK";
    }
}
