// TARGET_BACKEND: JVM
// SKIP_JDK6
// FILE: I.java
interface I {
    default String ifun() { return "fail"; }
}

// FILE: Z.java
public class Z {
    public String ifun() { return "OK"; }
}

// FILE: Zz.java
public class Zz extends Z implements I {}

// FILE: multipleImplFromJava.kt

class Cc : Zz()

fun box(): String = Zz().ifun()
