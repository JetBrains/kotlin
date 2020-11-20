// Not a multi-module test.
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_IR, JVM_MULTI_MODULE_OLD_AGAINST_IR, JVM_MULTI_MODULE_IR_AGAINST_OLD
// FILE: A.kt
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm
// WITH_RUNTIME
// FULL_JDK

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
