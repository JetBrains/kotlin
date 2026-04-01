// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
// FILE: main.kt
fun some() {
    val jClass = JavaClass()
    jClass.<caret>field;
}

// FILE: JavaClass.java
public class JavaClass {
    public int field = 1;
}
