// FILE: Box.java
public class Box<T extends Number> {
}

// FILE: JavaDeclaration.java
public interface JavaDeclaration {
    Box foo();
}

// FILE: main.kt
fun test(j: JavaDeclaration) = <expr>j.foo()</expr>
