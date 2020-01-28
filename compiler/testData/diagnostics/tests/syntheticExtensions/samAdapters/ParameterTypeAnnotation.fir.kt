// !WITH_NEW_INFERENCE
// FILE: KotlinFile.kt
fun foo(javaInterface: JavaInterface) {
    javaInterface.<!INAPPLICABLE_CANDIDATE!>doIt<!>(null) { }
    javaInterface.<!INAPPLICABLE_CANDIDATE!>doIt<!>("", null)
}

// FILE: JavaInterface.java
import org.jetbrains.annotations.*;

public interface JavaInterface {
    void doIt(@NotNull String s, @NotNull Runnable runnable);
}