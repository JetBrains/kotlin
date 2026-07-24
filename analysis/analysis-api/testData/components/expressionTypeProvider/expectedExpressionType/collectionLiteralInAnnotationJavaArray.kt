// FILE: TestAnnotation.java
public @interface TestAnnotation {
    String[] values();
}

// FILE: main.kt
@TestAnnotation(values = [<caret>x])
fun test() {}
