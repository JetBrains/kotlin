// FILE: main.kt
class A {
    fun foo() {
        <caret>JavaClass("")
    }
}

// FILE: JavaClass.java
import org.jetbrains.annotations.NotNull;

public class JavaClass {
    public JavaClass(@NotNull String s) {
    }
}