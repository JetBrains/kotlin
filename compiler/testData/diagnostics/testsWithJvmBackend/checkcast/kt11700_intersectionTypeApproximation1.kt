// TARGET_BACKEND: JVM_IR
// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: test.kt
import j.J
import j.PPImpl1
import j.PPImpl2

fun <T> select(a: T, b: T) = a

fun box(): String =
    J.test(
        <!IMPLICIT_CAST_TO_NON_ACCESSIBLE_CLASS!>select(PPImpl1(), PPImpl2())<!>
    )

// FILE: j/J.java
package j;

public class J {
    public static String test(PP pp) {
        return pp.test();
    }
}

// FILE: j/PA.java
package j;

public interface PA {
}

// FILE: j/PP.java
package j;

interface PP {
    String test();
}

// FILE: j/PPImpl1.java
package j;

public class PPImpl1 implements PA, PP {
    @Override
    public String test() {
        return "OK";
    }
}

// FILE: j/PPImpl2.java
package j;

public class PPImpl2 implements PA, PP {
    @Override
    public String test() {
        return "OK";
    }
}