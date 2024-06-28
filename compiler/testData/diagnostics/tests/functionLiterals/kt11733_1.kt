// FIR_IDENTICAL
// CHECK_TYPE

// FILE: Predicate.java
import org.jetbrains.annotations.NotNull;

public interface Predicate<T extends CharSequence> {
    // Same effect with @Nullable here
    boolean invoke(@NotNull T t);
}
// FILE: Main.kt
fun process(x: Predicate<String>) {}
fun main() {
    process(Predicate { x -> x checkType { _<String>() }
        true
    })
}
