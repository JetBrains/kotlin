// FIR_IDENTICAL
// JSPECIFY_STATE: warn
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-68110

// FILE: Entity.java
import org.jspecify.annotations.*;

public class Entity<T> {
    @Nullable
    public T getValue() {
        return null;
    }

    public static <R> void accept(R actual) {}
}

// FILE: test.kt
fun test(entity: Entity<Any>) {
    Entity.accept(entity.value)
}