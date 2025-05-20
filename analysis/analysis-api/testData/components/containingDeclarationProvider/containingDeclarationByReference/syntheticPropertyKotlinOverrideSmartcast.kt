// FILE: J.java
public abstract class J {
    public abstract String getString();
}

// FILE: test.kt
fun test(c: J) {
    if (c is K) {
        c.strin<caret>g
    }
}

class K : J() {
    override fun getString() = ""
}
