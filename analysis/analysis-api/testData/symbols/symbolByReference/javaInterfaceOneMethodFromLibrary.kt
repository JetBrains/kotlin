// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: JavaInterface.java
public interface JavaInterface {
    JavaInterface foo();
}

// MODULE: main(library)
// FILE: main.kt
fun some(j: <caret>JavaInterface) {
}
