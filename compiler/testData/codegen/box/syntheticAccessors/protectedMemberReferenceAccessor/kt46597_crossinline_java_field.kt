// TARGET_BACKEND: JVM

// FILE: a/Base.java
package a;

public class Base {
    protected String property = "OK";
}

// FILE: b.kt
import a.Base

class SubClass : Base() {
    fun call() =
        higherOrder(::property)

    inline fun higherOrder(crossinline lambda: () -> String) =
        lambda()
}

fun box() = SubClass().call()
