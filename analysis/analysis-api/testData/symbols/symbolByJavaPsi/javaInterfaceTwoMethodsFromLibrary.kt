// DO_NOT_CHECK_SYMBOL_RESTORE_K2
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: JavaInterface.java
public interface JavaInterface {
    JavaInterface foo();
    JavaInterface bar();
}

// MODULE: main(library)
// FILE: main.kt
fun some(j: <caret>JavaInterface) {
}
