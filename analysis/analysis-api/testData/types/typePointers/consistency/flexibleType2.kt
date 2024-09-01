// WITH_STDLIB

// FILE: JavaClass.java
import java.util.List;

public class JavaClass {
    public static List<String> get() {
        return null;
    }
}

// FILE: main.kt
fun some() = <expr>JavaClass.get()</expr>