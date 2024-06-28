// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// FILE: JavaAnnotation.java
public @interface JavaAnnotation {
    String name();
}

// FILE: main.kt
@JavaAnnotation(n<caret>ame = "")
fun test() {}
