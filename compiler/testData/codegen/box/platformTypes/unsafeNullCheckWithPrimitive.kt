// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// FILE: Unsound.java

import test.Wrap;

public class Unsound {
    public static <T> Wrap<T> get() {
        return new Wrap<T>(null);
    }
}

// FILE: 1.kt

package test

class Wrap<T>(val x: T)

// JVM IR generates bytecode that fails with NPE because it unwraps the value with `Number.intValue()`,
// whereas JVM generates a simple null check without unwrapping the box.
fun box(): String = if ((Unsound.get<Int>() as Wrap<Int>).x == null) "OK" else "Fail"
