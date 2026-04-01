// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
// FILE: JavaAnnotation.java
public @interface JavaAnnotation {
    String name();
}

// FILE: main.kt
@JavaAnnotation(n<caret>ame = "")
fun test() {}
