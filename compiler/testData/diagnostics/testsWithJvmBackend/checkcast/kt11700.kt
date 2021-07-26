// TARGET_BACKEND: JVM_IR
// FILE: test.kt
import j.J
import j.PPImpl

fun <T> T.id() = this

fun box(): String {
    return J.test(
        PPImpl()
            .<!IMPLICIT_CAST_TO_NON_ACCESSIBLE_CLASS!>id()<!>)
}

// FILE: j/PP.java
package j;

interface PP {
    String test();
}

// FILE: j/PPImpl.java
package j;

public class PPImpl implements PP {
    @Override
    public String test() {
        return "OK";
    }
}

// FILE: j/J.java
package j;

public class J {
    public static String test(PP pp) {
        return pp.test();
    }
}