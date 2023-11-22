// ISSUE: KT-62554
// FIR_DUMP
// SCOPE_DUMP: C:foo
// FILE: A.java

public class A {
    public void foo(Integer x) {}
}

// FILE: main.kt

interface B {
    fun foo(x: Int) {}
}

<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class C<!> : A(), B

fun main() {
    C().foo(42)
}
