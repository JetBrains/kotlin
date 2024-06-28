// FILE: main.kt
fun some() {
    JavaClass.<caret>count
}

// FILE: JavaClass.java
public class JavaClass {
    public static Integer count = 0;
}