// FIR_IDENTICAL
// JSPECIFY_STATE: warn
// FILE: Box.java

import org.jspecify.annotations.*;

@NullMarked
public class Box<V extends @Nullable Object> {
    public static <V extends @Nullable Object> Box<V> make() {
        return new Box<>();
    }
}

// FILE: Util.java
public final class Util {
    public Box<?> boxId(Box<?> box) { return box; }
}

// FILE: a.kt
fun foo(u: Util) =
    u.boxId(Box.make())
