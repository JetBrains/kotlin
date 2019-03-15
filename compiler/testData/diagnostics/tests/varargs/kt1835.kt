// FILE: JavaClass.java
public class JavaClass {
    void from(String s) {}
    void from(String... s) {}
}

// FILE: kotlin.kt
fun main() {
    JavaClass().from()
    JavaClass().from("")
    JavaClass().from("", "")
}