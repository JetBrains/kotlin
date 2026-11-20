// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: JavaDeclaration.java
import java.util.List;

public interface JavaDeclaration {
    List foo();
}

// MODULE: main(lib)
// FILE: main.kt
fun test(j: JavaDeclaration) = <expr>j.foo()</expr>
