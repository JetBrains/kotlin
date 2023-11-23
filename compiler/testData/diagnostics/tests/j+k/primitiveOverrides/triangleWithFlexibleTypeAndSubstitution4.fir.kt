// ISSUE: KT-62554
// FIR_DUMP
// SCOPE_DUMP: E:foo
// IGNORE_REVERSED_RESOLVE
// ^ resolves to C.foo instead of D.foo for some reason, does not seem important
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

<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class E<!> : C(), D

fun main() {
    E().foo(42)
}
