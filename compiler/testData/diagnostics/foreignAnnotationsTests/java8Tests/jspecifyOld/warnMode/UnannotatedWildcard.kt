// FIR_IDENTICAL
// JSPECIFY_STATE: warn
// FILE: J1.java
import org.jspecify.nullness.*;

@NullMarked
public interface J1<T extends @Nullable Object> {
    T foo();
}

// FILE: J2.java
public interface J2 {
    J1<?> bar();
}

// FILE: main.kt
fun baz(j2: J2): Any = j2.bar().foo() // Any..Any?
