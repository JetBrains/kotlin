// FILE: usage.kt
fun usa<caret>ge(j: JavaDeclaration) = j.foo()

// FILE: JavaDeclaration.java
public interface JavaDeclaration {
    Integer foo();
}