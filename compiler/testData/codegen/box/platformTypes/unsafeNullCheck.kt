// TARGET_BACKEND: JVM
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

fun box(): String = if ((Unsound.get<String>() as Wrap<String>).x == null) "OK" else "Fail"
