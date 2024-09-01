// FILE: main.kt
fun some() {
    JavaClass.<caret>count
}

// FILE: JavaClass.java
public class JavaClass {
    protected static Integer count = 0;
}
