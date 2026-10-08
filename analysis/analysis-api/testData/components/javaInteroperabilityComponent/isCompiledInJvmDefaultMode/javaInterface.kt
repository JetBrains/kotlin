// FILE: JavaInterface.java
public interface JavaInterface {
    default void foo() {}
}

// FILE: main.kt
fun usage(value: <expr>JavaInterface</expr>) {}
