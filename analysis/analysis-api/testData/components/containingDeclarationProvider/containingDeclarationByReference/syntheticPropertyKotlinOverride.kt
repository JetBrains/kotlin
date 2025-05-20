// FILE: J.java
public abstract class J {
    public abstract String getString();
}

// FILE: test.kt
fun test(k: K) {
    k.strin<caret>g
}

class K : J() {
    override fun getString() = ""
}
