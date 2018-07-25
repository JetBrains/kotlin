// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// FILE: JavaClass.java

public class JavaClass {

    public String runZ(Z z) {
        return z.run("O", "K");
    }

    protected interface Z {
        String run(String s1, String s2);
    }
}

// FILE: Kotlin.kt

package zzz

import JavaClass
import JavaClass.Z

class A : JavaClass() {
    fun test() = runZ(JavaClass.Z {a, b -> a + b})
}

fun box(): String {
    return A().test()
}
