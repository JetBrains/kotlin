// FIR_IDENTICAL
// JSPECIFY_STATE: strict
// !LANGUAGE: +TypeEnhancementImprovementsInStrictMode
// FILE: J1.java
import org.jetbrains.annotations.Nullable;

public interface J1<T> {
    @Nullable
    public static <T> T foo(J1<T> x) { return null; }
}

// FILE: J2.java
import org.jspecify.nullness.Nullable;

public interface J2<V extends @Nullable Object> extends J1<V> { }

// FILE: kotlin.kt
private fun J2<*>.bar() = J1.foo(this)