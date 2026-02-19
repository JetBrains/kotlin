// MODULE: m1
// FILE: JavaClass.java
public class JavaClass {
    public static String get() {
        return null;
    }
}

// FILE: main.kt
fun some() = <expr>JavaClass.get()</expr>

// MODULE: m2
// FILE: unrelated.kt
fun foo() {
    <caret_restoreAt>
}
