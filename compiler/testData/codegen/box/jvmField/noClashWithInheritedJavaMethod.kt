// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// MODULE: lib
// FILE: A.java
public class A {
    final public boolean isVisible() {
        return true;
    }
}

// FILE: B.kt
abstract class B : A() {
    @JvmField
    protected var isVisible = false
}

// MODULE: main(lib)
// FILE: box.kt
class C : B()

fun box(): String =
    if (C().isVisible()) "OK" else "Fail"
