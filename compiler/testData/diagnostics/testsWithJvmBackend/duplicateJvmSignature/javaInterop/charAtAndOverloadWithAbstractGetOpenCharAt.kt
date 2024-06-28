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

    public abstract char get(int index);
}

// FILE: B.kt
class B : A() {
    override <!ACCIDENTAL_OVERRIDE!>fun get(index: Int) = 'A'<!>
}
