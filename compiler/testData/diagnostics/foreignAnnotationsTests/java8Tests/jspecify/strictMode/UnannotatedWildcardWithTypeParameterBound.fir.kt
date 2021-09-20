// JSPECIFY_STATE: strict
// !LANGUAGE: +TypeEnhancementImprovementsInStrictMode

// FILE: J1.java
import org.jspecify.nullness.*;

@NullMarked
public interface J1<V, T extends @Nullable V> {
    T foo();
}

// FILE: J2.java
public interface J2 {
    J1<String, ?> bar(); // J1<String, out (String..String?)>
}

// FILE: main.kt
fun baz(j2: J2): String = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String!")!>j2.bar().foo()<!>
