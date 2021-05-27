// TARGET_BACKEND: JVM
// FILE: Base.java

import org.jetbrains.annotations.NotNull;

public abstract class Base {
    @NotNull
    protected String result = "OK";
}

// FILE: Derived.kt

open class Mid : Base()

class Derived : Mid() {
    fun foo(): String =
        (Derived::result)(this)
}

fun box(): String = Derived().foo()
