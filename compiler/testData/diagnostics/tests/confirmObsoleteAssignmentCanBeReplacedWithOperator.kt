// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-72197
// WITH_EXPERIMENTAL_CHECKERS

// FILE: Foo.java
public class Foo {
    // Only happens with synthetic properties
    public void setIntegerProp(int value) {}
    public int getIntegerProp() { return 0; }
}

// FILE: Main.kt
fun main() {
    val foo = Foo()
    val bar = 1L
    foo.integerProp = bar.toInt()
}
