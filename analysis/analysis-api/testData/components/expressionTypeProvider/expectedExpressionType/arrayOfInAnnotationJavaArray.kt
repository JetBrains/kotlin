// FILE: TestAnnotation.java
public @interface TestAnnotation {
    String[] values();
}

// FILE: main.kt
@TestAnnotation(values = arrayOf(<caret>x))
fun test() {}
