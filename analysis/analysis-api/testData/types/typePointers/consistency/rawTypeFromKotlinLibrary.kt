// MODULE: javaLib
// MODULE_KIND: LibraryBinary
// FILE: JavaDeclaration.java
import java.util.List;

public interface JavaDeclaration {
    List foo();
}

// MODULE: kotlinLib(javaLib)
// MODULE_KIND: LibraryBinary
// FILE: kotlinLib.kt
fun rawList(j: JavaDeclaration) = j.foo()

// MODULE: main(kotlinLib, javaLib)
// FILE: main.kt
fun test(j: JavaDeclaration) = <expr>rawList(j)</expr>
