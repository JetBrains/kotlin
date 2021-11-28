// FULL_JDK
// WITH_STDLIB
// TARGET_BACKEND: JVM
// ASSERTIONS_MODE: jvm
// FILE: A.kt

class A {
    inline fun inlineMe(crossinline c : () -> String) = {
        assert(true)
        c()
    }
}

// FILE: B.java
import kotlin.jvm.functions.Function0;

public class B {
    public static String check() {
        return new A().inlineMe(new Function0<String>() {
            @Override public String invoke() { return "OK"; }
        }).invoke();
    }
}

// FILE: box.kt
fun box(): String = B.check()
