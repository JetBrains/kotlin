// SCOPE_DUMP: KA:get
// RENDER_DIAGNOSTICS_FULL_TEXT

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
abstract class KA : A() {
    override fun <!ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE!>get<!>(index: Int) = 'O'
}

fun foo(a: A, ka: KA) {
    a.get(0)
    ka.get(0)
}
