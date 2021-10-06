// FILE: Java.java
import java.util.List

class Java {
    public static List<String> getNames() {
        throw Exception()
    }
}

// FILE: test.kt

val i = <expr>Java.getNames()</expr>