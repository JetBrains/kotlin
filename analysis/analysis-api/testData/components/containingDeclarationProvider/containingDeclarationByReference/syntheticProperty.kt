// FILE: J.java
public abstract class J {
    public abstract String getString();
}

// FILE: test.kt
fun test(j: J) {
    j.strin<caret>g
}
