// FILE: J.java
public class J extends K {
    @Override
    public String getString() { return ""; }
}

// FILE: test.kt
abstract class K {
    abstract val string: String
}

fun test(j: J) {
    j.strin<caret>g
}
