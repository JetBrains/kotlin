// ISSUE: KT-62554
// FIR_DUMP
// SCOPE_DUMP: E:foo
// FILE: A.java

public class A<T> {
    public String foo(T x) {
        return "A";
    }
}

// FILE: main.kt

interface B<T> {
    fun foo(x: T) = "B"
}

open class C : A<Int>()

interface D : B<Int>

class <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>E<!> : C(), D

fun main() {
    E().foo(42)
}
