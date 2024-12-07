// FILE: main.kt
fun some() {
    JavaClass().<caret>foo()
}

// FILE: JavaClass.java
public class JavaClass {
    protected void foo() {

    }
}
