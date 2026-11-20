// FILE: JavaDeclaration.java
import java.util.List;

public interface JavaDeclaration {
    List<List> foo();
}

// FILE: main.kt
fun test(j: JavaDeclaration) = <expr>j.foo()</expr>
