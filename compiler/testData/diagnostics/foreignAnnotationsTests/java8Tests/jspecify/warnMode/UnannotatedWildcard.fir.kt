// JSPECIFY_STATE: warn
// FILE: J1.java
import org.jspecify.annotations.*;

@NullMarked
public interface J1<T extends @Nullable Object> {
    T foo();
}

// FILE: J2.java
public interface J2 {
    J1<?> bar();
}

// FILE: main.kt
// jspecify_nullness_mismatch
fun baz(j2: J2): Any = <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>j2.bar().foo()<!> // Any..Any?
