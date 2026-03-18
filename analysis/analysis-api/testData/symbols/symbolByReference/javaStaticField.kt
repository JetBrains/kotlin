// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
// FILE: main.kt
fun some() {
    JavaClass.f<caret>ield;
}

// FILE: JavaClass.java
public class JavaClass {
    public static int field = 1;
}
