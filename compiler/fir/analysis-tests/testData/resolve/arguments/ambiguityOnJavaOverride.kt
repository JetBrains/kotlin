// FILE: A.java

public class A {
    Object foo(String s) {
        return null;
    };
}

// FILE: main.kt

class B : A() {
    override fun foo(s: String): Any? {
        return null
    }
}

fun test(b: B) {
    b.<!INVISIBLE_REFERENCE!>foo<!>("")
}
