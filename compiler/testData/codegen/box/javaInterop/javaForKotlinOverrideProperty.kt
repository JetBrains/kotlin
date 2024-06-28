// TARGET_BACKEND: JVM
// FILE: A.java

public abstract class A {
    public String getValue() {
        return "A";
    }
}

// FILE: I.kt

interface I {
    val value: String?
}

// FILE: B.java

public class B extends A implements I {}

// FILE: C.kt

class C : B(), I

fun box(): String {
    val c = C()
    if (c.value != "A") return "Fail C"
    if ((c as B).value != "A") return "Fail B"
    if ((c as I).value != "A") return "Fail I"
    if ((c as A).value != "A") return "Fail A value"
    if ((c as A).getValue() != "A") return "Fail A getValue"
    return "OK"
}
