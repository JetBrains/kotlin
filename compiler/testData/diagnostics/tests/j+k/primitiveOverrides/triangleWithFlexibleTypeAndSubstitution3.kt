// FIR_IDENTICAL
// ISSUE: KT-62554
// FIR_DUMP
// SCOPE_DUMP: D:foo
// FILE: A.java

public class A<T> {
    public String foo(T x) {
        return "A";
    }
}

// FILE: main.kt

interface B {
    fun foo(x: Int) = "B"
}

open class C : A<Int>()

class D : C(), B

fun main() {
    D().foo(42)
}
