// FILE: main.kt
fun some() {
    JavaClass().<caret>count
}

// FILE: JavaClass.java
public class JavaClass {
    protected Integer count = 0;
}
