// WITH_STDLIB
// IGNORE_FE10

// FILE: JavaClass.java
import java.util.List;

/**
 * Javadoc on class example
 */
public class JavaClass {
    /**
     * Javadoc on method example
     */
    public static List<String> get() {
        return null;
    }
}

// FILE: main.kt

fun foo() {
    JavaClass.get()
}