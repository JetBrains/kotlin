// FIR_IDENTICAL
// ISSUE: KT-62554
// FIR_DUMP
// SCOPE_DUMP: E:foo
// FILE: A.java

public class A {
    public String foo(Integer x) {
        return "A";
    }
}

// FILE: main.kt

interface B<T> {
    fun foo(x: T) = "B"
}

interface D : B<Int>

class E : A(), D

fun main() {
    E().foo(42)
}
