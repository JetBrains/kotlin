// KT-1730 Method which has been implemented by Java is recognized to be abstract.

// FILE: C.java
public class C implements java.lang.CharSequence {
    @Override
    public int length() {
        return 3;
    }
    @Override
    public char charAt(int index) {
        return 48;
    }
    @Override
    public CharSequence subSequence(int start, int end) {
        return "ab";
    }
    @Override
    public String toString() {
        return "abc";
    }
}

// FILE: T.kt
class T : C()
