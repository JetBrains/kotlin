// DO_NOT_CHECK_SYMBOL_RESTORE_K2
// FILE: main.kt
fun some(j: <caret>JavaInterface) {
}

// FILE: JavaInterface.java
public interface JavaInterface {
    JavaInterface foo();
    JavaInterface bar();
}
