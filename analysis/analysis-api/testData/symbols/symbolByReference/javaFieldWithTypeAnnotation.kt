// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// FILE: main.kt
@Target(AnnotationTarget.TYPE)
annotation class Anno1

fun some() {
    val jClass = JavaClass()
    jClass.<caret>field;
}

// FILE: JavaClass.java
public class JavaClass {
    public @Anno1 String field = 1;
}
