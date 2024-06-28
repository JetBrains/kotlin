// ISSUE: KT-66463
// SCOPE_DUMP: B:get

// FILE: A.java
public abstract class A implements CharSequence {
    public final int length() {
        return 0;
    }

    public char charAt(int index) {
        return 'K';
    }
    public final CharSequence subSequence(int startIndex, int endIndex) {
        return this;
    }

    public final char get(int index) {
        return 'X';
    }
}

// FILE: B.kt
class B : A() {
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun <!ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE!>get<!>(index: Int) = 'A'
}
