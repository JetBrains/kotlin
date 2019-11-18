// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: J.java

public class J {
    public static class A extends AImpl implements CharSequence {
        public CharSequence subSequence(int start, int end) {
            return null;
        }
    }
}

// FILE: test.kt

abstract class AImpl {
    fun charAt(index: Int): Char {
        return 'A'
    }

    fun length(): Int {
        return 56
    }
}

class X : J.A()

fun box(): String {
    val x = X()
    if (x.length != 56) return "fail 1"
    if (x[0] != 'A') return "fail 2"
    return "OK"
}
