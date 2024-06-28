// FIR_IDENTICAL
// SCOPE_DUMP: KA:get

// FILE: A.java
abstract public class A implements CharSequence {
    public final int length() {
        return 0;
    }

    public char charAt(int index) {
        return ' ';
    }

    public char get(int index) {
        return 'X';
    }
}

// FILE: main.kt
abstract class KA : A()

fun foo(a: A, ka: KA) {
    ka.get(0)
}
