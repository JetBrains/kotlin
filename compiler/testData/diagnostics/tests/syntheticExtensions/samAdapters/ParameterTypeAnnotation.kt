// FILE: KotlinFile.kt
fun foo(javaInterface: JavaInterface) {
    javaInterface.doIt(<!NULL_FOR_NONNULL_TYPE!>null<!>) <!TYPE_MISMATCH!>{ }<!>
    javaInterface.doIt("", <!NULL_FOR_NONNULL_TYPE!>null<!>)
}

// FILE: JavaInterface.java
import org.jetbrains.annotations.*;

public interface JavaInterface {
    void doIt(@NotNull String s, @NotNull Runnable runnable);
}