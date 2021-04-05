// !WITH_NEW_INFERENCE
// FILE: KotlinFile.kt
fun foo(javaInterface: JavaInterface) {
    javaInterface.doIt(<!ARGUMENT_TYPE_MISMATCH!>null<!>) { }
    javaInterface.doIt("", <!ARGUMENT_TYPE_MISMATCH!>null<!>)
}

// FILE: JavaInterface.java
import org.jetbrains.annotations.*;

public interface JavaInterface {
    void doIt(@NotNull String s, @NotNull Runnable runnable);
}