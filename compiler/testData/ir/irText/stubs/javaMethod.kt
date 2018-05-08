// !DUMP_DEPENDENCIES
// FILE: J.java

public class J {
    public void bar() {}
}

// FILE: javaMethod.kt

fun test(j: J) = j.bar()