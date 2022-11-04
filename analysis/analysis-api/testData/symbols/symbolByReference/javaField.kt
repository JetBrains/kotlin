// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// FILE: main.kt
fun some() {
    val jClass = JavaClass()
    jClass.<caret>field;
}

// FILE: JavaClass.java
public class JavaClass {
    public int field = 1;
}
