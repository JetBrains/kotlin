// Original problem was discovered in `backend.main/org/jetbrains/kotlin/codegen/inline/InlineCache.kt`

// FILE: SLRUMap.java

import org.jetbrains.annotations.NotNull;

public interface SLRUMap<V> {
    void takeV(@NotNull V value);
}

// FILE: main.kt

fun <V> SLRUMap<V>.getOrPut(value: V) {
    <!INAPPLICABLE_CANDIDATE!>takeV<!>(value)
}
