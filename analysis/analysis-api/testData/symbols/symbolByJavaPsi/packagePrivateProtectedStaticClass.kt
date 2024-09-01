// FILE: main.kt
fun some() {
    JavaClass.<caret>NestedClass()
}

// FILE: JavaClass.java
public class JavaClass {
    protected static class NestedClass {

    }
}
