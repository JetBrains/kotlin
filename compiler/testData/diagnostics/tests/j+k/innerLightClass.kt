// SKIP_TXT
// FIR_IDENTICAL
// FILE: A.java
import Outer.Middle;

public class A {
    public static void foo(Middle x) {}
    public static void bar(Middle.Innermost x) {}
    public static void baz(Outer x) {}
}

// FILE: main.kt

class Outer {
    class Middle {
        class Innermost
        fun main(r: Middle, i: Innermost, o: Outer) {
            A.foo(r)
            A.bar(i)
            A.baz(o)
        }
    }
}
