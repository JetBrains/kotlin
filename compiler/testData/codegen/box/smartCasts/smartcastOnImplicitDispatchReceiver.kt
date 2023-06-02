// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR
// ISSUE: KT-58823
// FILE: Base.java

public abstract class Base {}

// FILE: Derived.java
public class Derived extends Base {
    <T> T getResult() {
        return (T) "OK";
    }
}

// FILE: main.kt
fun Base.box(): String {
    return when (this) {
        is Derived -> getResult()
        else -> "Fail"
    }
}

fun box(): String = Derived().box()
