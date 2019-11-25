// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: J.java

import java.util.*;

public class J {

    public static class B extends A {
        public int getLength() { return 456; }
        public char get(int index) {
            if (index == 1) return 'a';
            return super.get(index);
        }
    }

    public static String foo() {
        B b = new B();
        CharSequence cs = (CharSequence) b;

        if (cs.length() != 456) return "fail 01";
        if (b.length() != 456) return "fail 02";
        if (b.getLength() != 456) return "fail 03";

        if (cs.charAt(0) != 'z') return "fail 1";
        if (b.get(0) != 'z') return "fail 2";

        if (cs.charAt(1) != 'a') return "fail 3";
        if (b.get(1) != 'a') return "fail 4";

        return "OK";
    }
}

// FILE: test.kt

open class A : CharSequence {
    override val length: Int = 123

    override fun get(index: Int) = 'z';

    override fun subSequence(start: Int, end: Int): CharSequence {
        throw UnsupportedOperationException()
    }
}

fun box(): String {
    val b = J.B()
    val a = A()

    if (b[0] != 'z') return "fail 6"
    if (a[0] != 'z') return "fail 7"
    if (b[1] != 'a') return "fail 8"
    if (a[0] != 'z') return "fail 9"

    if (b.get(0) != 'z') return "fail 10"
    if (a.get(0) != 'z') return "fail 11"
    if (b.get(1) != 'a') return "fail 12"
    if (a.get(1) != 'z') return "fail 13"

    var cs: CharSequence = a
    if (a.length != 123) return "fail 14"
    if (cs.length != 123) return "fail 15"

    cs = b
    if (b.length != 456) return "fail 16"
    if (b.length != 456) return "fail 17"

    return J.foo();
}
