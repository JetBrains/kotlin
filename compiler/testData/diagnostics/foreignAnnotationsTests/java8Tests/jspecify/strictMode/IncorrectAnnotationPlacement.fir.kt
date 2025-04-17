// JSPECIFY_STATE: strict
// ISSUE: KT-53836

// FILE: J.java
import org.jspecify.annotations.*;

public interface J<@Nullable T> {
    T get();
}

// FILE: main.kt
fun go(j: J<*>): Any = j.get()
