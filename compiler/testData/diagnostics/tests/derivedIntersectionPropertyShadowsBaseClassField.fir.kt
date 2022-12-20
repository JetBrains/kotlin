// WITH_STDLIB
// FIR_DUMP
// FILE: Base.java

public class Base {
    public String x = "";
}

// FILE: test.kt

interface Proxy {
    val x: String
}

open class Intermediate : Base() {
    val x get() = " "
}

class Derived : Proxy, Intermediate() {
    fun test() {
        x
    }
}