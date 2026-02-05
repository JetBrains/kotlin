// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: one/two/Bar.java

package one.two;

public class Bar {
    public static final int BAR = OtherKt.FOO + 1;

    public Child getChild() {
        return new Child();
    }
}

// FILE: Main.kt

@file:JvmName("OtherKt")

package one.two

const val FOO = 1

const val BAZ = Bar.BAR + 1

// This class is presented here to check that on super type resolve phase we have resolved `JvmName` annotation
class Child : Bar()

fun box(): String {
    return "OK"
}
