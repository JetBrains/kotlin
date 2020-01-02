// JAVAC_SKIP
// FILE: A.java
public class A {
    public B b() {}
    public F f() {}
}

class B { public void bar() {} }

// FILE: C.java
class D {
    public void baz() {}
}

// FILE: E.java
class F {
    public void foobaz() {}
}

// FILE: main.kt
fun main(x: A) {
    x.b().<!UNRESOLVED_REFERENCE!>bar<!>()
    x.f().<!UNRESOLVED_REFERENCE!>foobaz<!>()

    <!UNRESOLVED_REFERENCE!>D<!>().<!UNRESOLVED_REFERENCE!>baz<!>()
}
