// IGNORE_BACKEND: JKLIB
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FILE: A.kt
interface A {
    var x: String
}

// FILE: B.java
public class B implements A {
    @Override
    public String getX() { return null; }

    @Override
    public final void setX(String s) {}
}

// FILE: test.kt
fun test(b: B) {
    b.x = ""
    b.getX()
    b.setX("")
}
