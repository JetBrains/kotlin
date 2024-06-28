// FILE: main.kt
fun some() {
    JavaClass().<caret>InnerClass()
}

// FILE: JavaClass.java
public class JavaClass {
    protected class InnerClass {

    }
}
