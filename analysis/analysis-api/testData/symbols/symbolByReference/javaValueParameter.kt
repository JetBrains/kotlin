// FILE: JavaAnnotation.java
public @interface JavaAnnotation {
    String name();
}

// FILE: main.kt
@JavaAnnotation(n<caret>ame = "")
fun test() {}
