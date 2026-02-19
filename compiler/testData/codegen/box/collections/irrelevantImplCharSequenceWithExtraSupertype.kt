// TARGET_BACKEND: JVM
// DUMP_IR
// DUMP_EXTERNAL_CLASS: J.A
// FILE: J.java

public class J {
    abstract static public class AImpl {
        public char charAt(int index) {
            return 'A';
        }

        public final int length() { return 56; }
    }

    public static class A extends AImpl implements CharSequence {
        public CharSequence subSequence(int start, int end) {
            return null;
        }
    }
}

// FILE: test.kt

interface I : CharSequence

class X : J.A(), I

fun box(): String {
    val x = X()
    if (x.length != 56) return "fail 1"
    if (x[0] != 'A') return "fail 2"
    return "OK"
}
