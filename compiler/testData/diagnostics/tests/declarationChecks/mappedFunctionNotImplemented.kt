// KT-48102
// FIR_IDENTICAL

// FILE: MyCharSequence.java
public class MyCharSequence implements CharSequence {
    public char charAt(int index) { return ' '; }
    public int length() { return 1; }
    public CharSequence subSequence(int start, int end) { return this; }
    public String toString() { return " "; }
}

// FILE: CharSeq.kt
class KtCharSeq : MyCharSequence() // false-positive 'get' not implemented