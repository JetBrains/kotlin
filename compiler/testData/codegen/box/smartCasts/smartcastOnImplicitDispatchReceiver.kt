// TARGET_BACKEND: JVM
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
