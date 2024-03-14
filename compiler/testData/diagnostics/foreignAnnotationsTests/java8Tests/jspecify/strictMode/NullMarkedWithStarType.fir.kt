// JSPECIFY_STATE: strict
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
    u.boxId(Box.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>make<!>())
