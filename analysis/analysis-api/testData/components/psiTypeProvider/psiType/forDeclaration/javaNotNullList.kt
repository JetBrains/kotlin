// FILE: usage.kt
fun usa<caret>ge(j: JavaDeclaration) = j.foo()

// FILE: JavaDeclaration.java
import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface JavaDeclaration {
    @NotNull List<String> foo();
}
