// FILE: usage.kt
fun usa<caret>ge(j: JavaDeclaration) = j.foo()

// FILE: JavaDeclaration.java
import java.util.List;

public interface JavaDeclaration {
    List<String> foo();
}
