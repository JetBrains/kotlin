// FILE: JavaClass.java
public class JavaClass {
    public static int STATIC_FIELD = 1;

    public int instanceField = 2;

    public static void staticMethod() {}

    public void instanceMethod() {}
}

// FILE: main.kt
fun test(javaClass: Java<caret>Class) {}
